package pdfRead;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO １．ログ出力
// TODO ２．エラーハンドリング
// TODO ３．データパターン
// TODO ４．設定ファイルの外だし
// TODO ５．もう少しよいロジックがあるはず

public class pdfBox1 {

	public static String INPUT_PATH = "";
	public static String OUTPUT_PATH = "";
	public static String DEFINE_ITEM_FILE = "";
	public static String DEFINE_HEAD_FILE = "";
	public static final String PDF = ".pdf";
	public static final String CSV = ".csv";
	public static final String AST = "(*)";
	public static Logger logger = LoggerFactory.getLogger(pdfBox1.class);

	/**
	 * メイン処理
	 * 
	 * @param args
	 *            ファイル名(xxx.pdf) - 第一引数のみ使用
	 */
	public static void main(String... args) {

		// 引数チェック
		if (args.length != 1) {
			logger.error("引数が不正です。{}", Arrays.toString(args));
		} else {
			String filename = args[0].replace(PDF, "");

			// ヘッダ定義ファイル格納用
			LinkedHashMap<Integer, String> headerMap = new LinkedHashMap<Integer, String>();
			// 項目定義ファイル格納用
			LinkedHashMap<Integer, LinkedHashMap<String, String>> itemListMap = new LinkedHashMap<Integer, LinkedHashMap<String, String>>();
			List<String> pdfData = new ArrayList<String>();

			logger.info("START - {}", args[0]);

			try {
				// 改行コード取得
				String linecode = getLineCode();
				// 外部プロパティファイル読み込み
				readProperties();

				String inputFile = INPUT_PATH + args[0];
				String outputFile = OUTPUT_PATH + filename + CSV;

				// ヘッダ定義ファイル読み込み
				readHeader(DEFINE_HEAD_FILE, headerMap);
				// 項目定義ファイル読み込み
				readItem(DEFINE_ITEM_FILE, itemListMap);
				// PDFファイル読み込み
				readPDFFile(inputFile, pdfData, linecode);

				// 出力対象データの抜き出し
				List<Map<String, String>> dataMap = targetDataExtra(headerMap, itemListMap, pdfData);

				// 出力対象データ加工
				Map<String, String[]> dataList = dataProcessing(dataMap);

				if (pdfData.isEmpty() || dataList.isEmpty()) {
					logger.info("PDFファイルのフォント等を確認してください。　ファイル名：{}", args[0]);
				}
				// ファイル出力
				outputResult(dataList, outputFile);

			} catch (Exception e) {
				logger.error("システムエラーが発生しました", e);
			}

			logger.info("END - {}", args[0]);
		}
	}

	/**
	 * 改行コードを取得する
	 * @return 改行コード
	 */
	private static String getLineCode() {
		String lc = System.getProperty("line.separator");
		return lc;
	}

	/**
	 * 出力ファイル（CSV）に合わせて、カンマ区切り＋ダブコでデータを区切る
	 * 
	 * @param dataList
	 *            出力対象データ
	 * @param outputFile
	 *            出力対象ファイルパス
	 * @throws IOException
	 *             入出力処理による例外
	 */
	private static void outputResult(Map<String, String[]> dataList, String outputFile) throws IOException {
		File f = new File(outputFile);
		PrintWriter pw = new PrintWriter(
				new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "Shift-JIS")));

		for (Iterator<Entry<String, String[]>> it = dataList.entrySet().iterator(); it.hasNext();) {
			Entry<String, String[]> entry = it.next();

			pw.print("\"" + entry.getKey() + "\"");

			for (int i = 0; i < entry.getValue().length; i++) {
				pw.print(",");
				pw.print("\"" + entry.getValue()[i] + "\"");
			}
			pw.println();
		}

		pw.close();
	}

	/**
	 * 出力対象データの加工
	 * 
	 * @param dataMap
	 *            出力対象データ
	 * @return 加工後の出力対象データ
	 */
	private static Map<String, String[]> dataProcessing(List<Map<String, String>> dataMap) {
		Map<String, String[]> map = new LinkedHashMap<String, String[]>();

		for (Iterator<Map<String, String>> it = dataMap.iterator(); it.hasNext();) {
			for (Iterator<Entry<String, String>> itt = it.next().entrySet().iterator(); itt.hasNext();) {
				Entry<String, String> entry = itt.next();

				// DEBUG
				// System.out.println(entry.getKey() + " : " + entry.getValue());
				logger.debug("{} : {}", entry.getKey(), entry.getValue());

				String[] str = dataTrim(entry.getKey(), entry.getValue());
				map.put(entry.getKey(), str);
			}
		}
		return map;
	}

	/**
	 * 出力対象データをトリム（ValueからKeyを削除）
	 * 
	 * @param key
	 *            MapのKey
	 * @param value
	 *            MapのValue
	 * @return トリム後のデータ
	 */
	private static String[] dataTrim(String key, String value) {
		String line = null;
		String[] data = null;
		String str = key + AST;

		line = value.replace(str, "");
		line = line.replace(key, "");

		line = line.trim();
		data = line.split("[\\s]+");

		return data;
	}

	/**
	 * PDFデータから出力対象となる部分を抜き出す
	 * 
	 * @param headerMap
	 *            ヘッダ定義データ
	 * @param itemListMap
	 *            項目定義データ
	 * @param pdfData
	 *            PDFデータ
	 * @return 出力対象データ
	 */
	private static List<Map<String, String>> targetDataExtra(LinkedHashMap<Integer, String> headerMap,
			LinkedHashMap<Integer, LinkedHashMap<String, String>> itemListMap, List<String> pdfData) {

		List<Map<String, String>> data = new ArrayList<Map<String, String>>();
		Map<String, String> map = new LinkedHashMap<String, String>();
		String strData = null;
		// Header
		for (Iterator<Entry<Integer, String>> it_head = headerMap.entrySet().iterator(); it_head.hasNext();) {
			Entry<Integer, String> entryHeaderMap = it_head.next();

			boolean isSearch = false; // Header検索用 （余計な検索を省略するため）
			boolean isRepeat = false; // 読取対象が複数行あるもの
			String strKey = null;
			String strOrgKey = null;
			int lineCounter = 1;

			// Header定義がPDFデータに存在しているか
			// PDFデータ
			for (Iterator<String> it_pdf = pdfData.iterator(); it_pdf.hasNext();) {
				String strPdfdata = it_pdf.next();

				Map<String, String> itemMap = itemListMap.get(entryHeaderMap.getKey());

				if (!isSearch) {
					// TODO 部分一致？前方一致？
					// if (strPdfdata.startsWith(entryHeaderMap.getValue())) {
					if (strPdfdata.indexOf(entryHeaderMap.getValue()) != -1) {
						// 存在した場合、次の行以降を検索
						isSearch = true;
					}
					continue;
				}

				// 読み取り対象が複数行にわたる場合に、一行にする
				if (lineCounter > 1) {
					strData = strData.concat(strPdfdata);
					lineCounter--;
					continue;
				}
				if (isRepeat) {
					// if (strData.indexOf(strKey) != -1) {
					if (strData.startsWith(strKey)) {
						map.put(strKey, strData);
						itemMap.remove(strOrgKey);
					}
					lineCounter = 1;
					isRepeat = false;
				}

				// 存在した場合、項目定義データを取り出す
				for (Iterator<Entry<String, String>> it_item = itemMap.entrySet().iterator(); it_item.hasNext();) {
					Entry<String, String> entryItemData = it_item.next();
					String str = entryItemData.getKey().split("\\|")[0];
					if (strPdfdata.startsWith(str) && !isRepeat) {
						if (!"1".equals(entryItemData.getValue().split("-")[1])) {
							strOrgKey = entryItemData.getKey();
							strKey = entryItemData.getKey().replaceAll("\\|", "").replaceAll("\\s{2,}", " ").trim();
							strData = strPdfdata;
							lineCounter = Integer.parseInt(entryItemData.getValue().split("-")[1]);
							isRepeat = true;
							// it_item.remove();
							continue;
						}

						map.put(entryItemData.getKey(), strPdfdata);
						it_item.remove();

						if ("E".equals(entryItemData.getValue().split("-")[0])) {
							data.add(map);
							return data;
						}
					}
				}
			}
		}
		data.add(map);
		return data;
	}

	/**
	 * PDFファイルの読み込み
	 * 
	 * @param inputFile
	 *            PDFファイルパス
	 * @param pdfData
	 *            読み込み結果格納用
	 * @param linecode 改行コード
	 * @throws IOException
	 *             入出力処理による例外
	 */
	private static void readPDFFile(String inputFile, List<String> pdfData, String linecode) throws IOException {
		File file = new File(inputFile);
		try (PDDocument document = PDDocument.load(file)) {
			PDFTextStripper s = new PDFTextStripper();
			String text = s.getText(document);

			// DEBUG
			// System.out.println("[" + text + "]");
			logger.debug(text);

			String[] str = text.split(linecode);
			for (int i = 0; i < str.length; i++) {
				pdfData.add(str[i]);
			}
			document.close();
		}
	}

	/**
	 * 項目定義ファイルの読み込み
	 * 
	 * @param defineFile
	 *            項目定義ファイルパス
	 * @param map2
	 *            読み込み結果格納用
	 * @throws IOException
	 *             入出力処理による例外
	 */
	private static void readItem(String defineFile, LinkedHashMap<Integer, LinkedHashMap<String, String>> map2)
			throws IOException {
		File file = new File(defineFile);
		String line = null;
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));

		while ((line = br.readLine()) != null) {
			String[] str = line.split(",");

			// str[0]: Type
			// str[1]: Start/End - Repeat
			// str[2]: Item Name
			int a = Integer.parseInt(str[0]);
			map.put(str[2], str[1]);

			if ("E".equals(str[1].split("-")[0])) {
				map2.put(a, map);
				map = new LinkedHashMap<String, String>();
			}
		}
		br.close();
	}

	/**
	 * ヘッダ定義ファイルの読み込み
	 * 
	 * @param defineHeadFile
	 *            ヘッダ定義ファイルパス
	 * @param headerMap
	 *            読み込み結果格納用
	 * @throws IOException
	 *             入出力処理による例外
	 */
	private static void readHeader(String defineHeadFile, LinkedHashMap<Integer, String> headerMap) throws IOException {
		File file = new File(defineHeadFile);
		String line = null;
		BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));

		while ((line = br.readLine()) != null) {
			String[] str = line.split(",");

			// str[0]: Type
			// str[1]: Header Name
			int a = Integer.parseInt(str[0]);
			headerMap.put(a, str[1]);
		}
		br.close();
	}

	/**
	 * プロパティファイル（ファイルパス設定用）の読み込み
	 * @throws Exception 例外発生時
	 */
	private static void readProperties() throws Exception {
		
		ProtectionDomain pd = pdfBox1.class.getProtectionDomain();
		CodeSource cs = pd.getCodeSource();
		URL location = cs.getLocation();
		URI uri = location.toURI();
		// 実行ファイルの１つ上（親）のファイルパス
		Path path = Paths.get(uri).getParent();
		String strPath = new File(path.toString(), "resources").getPath();
		
		URLClassLoader urlLoader = new URLClassLoader(new URL[] {new File(strPath).toURI().toURL()});
		ResourceBundle rb = ResourceBundle.getBundle("filepath", Locale.getDefault(), urlLoader);
		
		INPUT_PATH = rb.getString("input.filepath");
		OUTPUT_PATH = rb.getString("output.filepath");
		DEFINE_HEAD_FILE = rb.getString("header.file");
		DEFINE_ITEM_FILE = rb.getString("item.file");
	}
}
