"""
① 개선 단계별 구성을 재현하는 jar 를 만든다. 세 가지 개선(Bulk Insert, 트랜잭션 분리, DataFormatter 재사용)이
한 커밋(52ed226)에 함께 들어가 단계별 커밋이 없으므로, 현재 코드(S3~S5 수정 후)에서 해당 부분만 되돌린다.
바꾼 내용은 patches/<구성>.patch 로 남기고 소스는 원래대로 되돌린다.

  v0   개선 전: 파싱까지 한 트랜잭션 + JPA saveAll(IDENTITY 라 행마다 INSERT) + 셀마다 DataFormatter 생성
  v1   병목 1 해결: v0 에서 저장만 JdbcTemplate.batchUpdate 로
  v2a  트랜잭션 분리: v1 에서 파싱을 트랜잭션 밖으로(DataFormatter 는 여전히 셀마다 생성)
  (v2  현재 코드: v2a + DataFormatter 재사용)

사용: python build-variant-jars.py
"""
import pathlib
import shutil
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[3]
API = ROOT / "apps" / "api-server"
PKG = API / "src/main/java/growup/spring/springserver/marginforcampaign"
PARSING = PKG / "service/ExcelParsingService.java"
SERVICE = PKG / "service/ExcelService.java"
CONTROLLER = PKG / "controller/ExcelDownUpLoadController.java"
OUT = API / "build" / "excel-jars"
PATCHES = pathlib.Path(__file__).resolve().parent / "patches"

PER_CELL_FORMATTER = (PARSING, "yield formatter.formatCellValue(cell);",
                      "yield new DataFormatter().formatCellValue(cell);")
ONE_TX_METHOD = (SERVICE, "    private void saveMfcInExcel(", """    // 개선 전 구조: 파싱부터 쓰기까지 한 트랜잭션
    @Transactional
    public Map<String,Integer> parseAndProcessInOneTransaction(ExcelParsingService excelParsingService,
                                                               MultipartFile file, String email) throws IOException {
        return processUploadedExcel(excelParsingService.parsingExcelData(file, email));
    }

    private void saveMfcInExcel(""")
ONE_TX_IMPORT = (SERVICE, "import org.springframework.util.StopWatch;\n",
                 "import org.springframework.util.StopWatch;\nimport org.springframework.web.multipart.MultipartFile;\n")
ONE_TX_CALL = (CONTROLLER, """            ExcelUploadContext excelUploadContext = excelParsingService.parsingExcelData(file, userDetails.getUsername());
            Map<String,Integer> result = excelService.processUploadedExcel(excelUploadContext);""",
               """            Map<String,Integer> result = excelService.parseAndProcessInOneTransaction(excelParsingService, file, userDetails.getUsername());""")
SAVE_ALL = (SERVICE, """        saveMfcInExcel(excelUploadContext.getEntitiesToInsert());
        bulkUpdateMfc(excelUploadContext.getEntitiesToUpdate());""",
            """        marginForCampaignRepository.saveAll(excelUploadContext.getEntitiesToInsert());
        marginForCampaignRepository.saveAll(excelUploadContext.getEntitiesToUpdate());""")

VARIANTS = {
    "v0": [PER_CELL_FORMATTER, ONE_TX_METHOD, ONE_TX_IMPORT, ONE_TX_CALL, SAVE_ALL],
    "v1": [PER_CELL_FORMATTER, ONE_TX_METHOD, ONE_TX_IMPORT, ONE_TX_CALL],
    "v2a": [PER_CELL_FORMATTER],
}


def main():
    files = {PARSING, SERVICE, CONTROLLER}
    originals = {f: f.read_text(encoding="utf-8") for f in files}
    for edits in VARIANTS.values():
        for f, old, _ in edits:
            assert originals[f].count(old) == 1, f"코드가 바뀌어 패치를 적용할 수 없습니다: {f.name}: {old[:40]}"
    OUT.mkdir(parents=True, exist_ok=True)
    PATCHES.mkdir(exist_ok=True)
    gradlew = str(API / ("gradlew.bat" if shutil.which("cmd") else "gradlew"))
    try:
        for name, edits in VARIANTS.items():
            texts = dict(originals)
            for f, old, new in edits:
                texts[f] = texts[f].replace(old, new)
            for f, text in texts.items():
                f.write_text(text, encoding="utf-8", newline="")
            diff = subprocess.run(["git", "diff", "--", *map(str, files)], cwd=ROOT, capture_output=True,
                                  text=True, encoding="utf-8").stdout
            (PATCHES / f"{name}.patch").write_text(diff, encoding="utf-8", newline="")
            subprocess.run([gradlew, "bootJar", "-q"], cwd=API, check=True)
            jar = next(p for p in (API / "build" / "libs").glob("*-SNAPSHOT.jar") if "plain" not in p.name)
            shutil.copy(jar, OUT / f"{name}.jar")
            print(f"{name}: {OUT / (name + '.jar')}")
    finally:
        for f, text in originals.items():
            f.write_text(text, encoding="utf-8", newline="")
        subprocess.run([gradlew, "bootJar", "-q"], cwd=API, check=True)  # 기본 jar 를 현재 코드로 되돌린다


if __name__ == "__main__":
    main()
