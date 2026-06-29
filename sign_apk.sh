#!/usr/bin/env bash
# =============================================================
#  sign_apk.sh — Full APK build, sign, zipalign pipeline
#  Usage: ./sign_apk.sh [OPTIONS]
#
#  Options:
#    --keystore <path>     Path to .jks keystore (will create if missing)
#    --alias <name>        Key alias (default: release-key)
#    --apk <path>          Path to unsigned APK (skip if using Gradle)
#    --gradle              Run ./gradlew assembleRelease before signing
#    --output <path>       Output APK path (default: ./app-release-final.apk)
#    --gen-keystore        Force regenerate keystore even if it exists
#    --help                Show this help
# =============================================================

set -euo pipefail

# ─── Colors ─────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

log()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
success(){ echo -e "${GREEN}[OK]${NC}    $*"; }
warn()   { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()  { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }
header() { echo -e "\n${BOLD}━━━ $* ━━━${NC}"; }

# ─── Defaults ───────────────────────────────────────────────
KEYSTORE_PATH="./release-key.jks"
KEY_ALIAS="release-key"
UNSIGNED_APK=""
OUTPUT_APK="./app-release-final.apk"
USE_GRADLE=false
FORCE_KEYGEN=false

# ─── Arg Parsing ────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --keystore)    KEYSTORE_PATH="$2"; shift 2 ;;
    --alias)       KEY_ALIAS="$2";     shift 2 ;;
    --apk)         UNSIGNED_APK="$2";  shift 2 ;;
    --gradle)      USE_GRADLE=true;    shift ;;
    --output)      OUTPUT_APK="$2";    shift 2 ;;
    --gen-keystore)FORCE_KEYGEN=true;  shift ;;
    --help)
      sed -n '3,12p' "$0" | sed 's/#//g'
      exit 0 ;;
    *) error "Unknown option: $1. Use --help for usage." ;;
  esac
done

# ─── Ensure Directories Exist ───────────────────────────────
mkdir -p keys dist

# ─── Dependency Checks ──────────────────────────────────────
header "Checking dependencies"

check_cmd() {
  if ! command -v "$1" &>/dev/null; then
    error "'$1' not found. $2"
  fi
  success "$1 found"
}

check_cmd keytool  "Install JDK: sudo apt install default-jdk"
check_cmd apksigner "Install Android build-tools or set \$ANDROID_HOME correctly.
         Alternatively: sudo apt install apksigner"
check_cmd zipalign  "Install Android build-tools or: sudo apt install zipalign"

# ─── Step 1: Generate Keystore ──────────────────────────────
header "Step 1: Keystore"

if [[ -f "$KEYSTORE_PATH" && "$FORCE_KEYGEN" == false ]]; then
  warn "Keystore already exists at '$KEYSTORE_PATH' — skipping generation."
  warn "Use --gen-keystore to force regenerate."
else
  log "Generating keystore at: $KEYSTORE_PATH"
  echo ""
  warn "You'll be prompted for keystore details. Fill them in (or press Enter for defaults)."
  echo ""

  keytool -genkey -v \
    -keystore "$KEYSTORE_PATH" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -alias "$KEY_ALIAS"

  success "Keystore created: $KEYSTORE_PATH"
fi

# ─── Step 2: Build with Gradle (optional) ───────────────────
if [[ "$USE_GRADLE" == true ]]; then
  header "Step 2: Gradle Build"

  if [[ ! -f "./gradlew" ]]; then
    error "gradlew not found in current directory. Run from your Android project root."
  fi

  log "Running: ./gradlew assembleRelease"
  chmod +x ./gradlew
  ./gradlew assembleRelease

  # Auto-detect unsigned APK if not specified
  if [[ -z "$UNSIGNED_APK" ]]; then
    UNSIGNED_APK=$(find app/build/outputs/apk/release -name "*unsigned*.apk" 2>/dev/null | head -1)
    if [[ -z "$UNSIGNED_APK" ]]; then
      # Some setups don't suffix with "unsigned"
      UNSIGNED_APK=$(find app/build/outputs/apk/release -name "*.apk" 2>/dev/null | head -1)
    fi
    [[ -z "$UNSIGNED_APK" ]] && error "Could not find built APK. Specify it with --apk <path>"
    log "Auto-detected APK: $UNSIGNED_APK"
  fi

  success "Gradle build complete"
else
  header "Step 2: APK Input"
  [[ -z "$UNSIGNED_APK" ]] && error "No APK specified. Use --apk <path> or --gradle to build first."
  [[ ! -f "$UNSIGNED_APK" ]] && error "APK not found: $UNSIGNED_APK"
  log "Using APK: $UNSIGNED_APK"
fi

# ─── Step 3: Zipalign BEFORE signing (apksigner requirement) ─
header "Step 3: Zipalign"

ALIGNED_APK="${UNSIGNED_APK%.apk}-aligned.apk"

log "Aligning APK..."
zipalign -v -f 4 "$UNSIGNED_APK" "$ALIGNED_APK"
success "Aligned APK: $ALIGNED_APK"

# ─── Step 4: Sign ───────────────────────────────────────────
header "Step 4: Sign APK"

log "Signing with keystore: $KEYSTORE_PATH (alias: $KEY_ALIAS)"
log "Output: $OUTPUT_APK"
echo ""

apksigner sign \
  --ks "$KEYSTORE_PATH" \
  --ks-key-alias "$KEY_ALIAS" \
  --out "$OUTPUT_APK" \
  "$ALIGNED_APK"

success "Signed APK: $OUTPUT_APK"

# ─── Step 5: Verify ─────────────────────────────────────────
header "Step 5: Verify Signature"

apksigner verify --verbose "$OUTPUT_APK"
success "Signature verified!"

# ─── Cleanup ────────────────────────────────────────────────
log "Cleaning up intermediate aligned APK..."
rm -f "$ALIGNED_APK"

# ─── Summary ────────────────────────────────────────────────
echo ""
echo -e "${BOLD}${GREEN}✅ Done! Summary:${NC}"
echo -e "  Keystore  : ${BOLD}$KEYSTORE_PATH${NC}"
echo -e "  Key Alias : ${BOLD}$KEY_ALIAS${NC}"
echo -e "  Final APK : ${BOLD}$OUTPUT_APK${NC}"
echo ""
warn "Keep your .jks keystore file backed up securely — you CANNOT re-upload to Play Store without it."
