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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

// TODO １．ログ出力
// TODO ２．エラーハンドリング
// TODO ３．データパターン
// TODO ４．設定ファイルの外だし
// TODO ５．もう少しよいロジックがあるはず

public class pdfBox1 {
	
	public static final String INPUT_PATH = "C:\\test\\pdf\\";
	public static final String OUTPUT_PATH = "C:\\test\\result\\";
	public static final String DEFINE_ITEM_FILE = "C:\\test\\item\\item.txt";
	public static final String DEFINE_HEAD_FILE = "C:\\test\\item\\header.txt";
	public static final String PDF = ".pdf";
	public static final String CSV = ".csv";
	public static final String AST = "(*)";
	
	public static void main(String...args) throws IOException {
		
		if (args.length != 1) {
			System.out.println("引数エラーです");
		} else {
			String filename = args[0].replace(PDF, "");
			String inputFile = INPUT_PATH + args[0];
			String outputFile = OUTPUT_PATH + filename + CSV;

			// ヘッダ定義ファイル格納用
			LinkedHashMap<Integer, String> headerMap = new LinkedHashMap<Integer, String>();
			// 項目定義ファイル格納用
			LinkedHashMap<Integer, LinkedHashMap<String, String>> itemListMap = new LinkedHashMap<Integer, LinkedHashMap<String, String>>();
			List<String> pdfData = new ArrayList<String>();

			// ヘッダ定義ファイル読み込み
			readHeader(DEFINE_HEAD_FILE, headerMap);
			// 項目定義ファイル読み込み
			readItem(DEFINE_ITEM_FILE, itemListMap);
			// PDFファイル読み込み
			readPDFFile(inputFile, pdfData);
			// 出力対象データの抜き出し
			List<Map<String, String>> dataMap = targetDataExtra(headerMap, itemListMap, pdfData);
			// 出力対象データ加工
			Map<String, String[]> dataList = dataProcessing(dataMap);
			// ファイル出力
			outputResult(dataList, outputFile);
		}
	}

	private static void outputResult(Map<String, String[]> dataList, String outputFile) throws IOException {
		File f = new File(outputFile);
		PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f),"Shift-JIS")));
		
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

	private static Map<String, String[]> dataProcessing(List<Map<String, String>> dataMap) {
		Map<String, String[]> map = new LinkedHashMap<String, String[]>();
		
		for (Iterator<Map<String, String>> it = dataMap.iterator(); it.hasNext();) {
			for (Iterator<Entry<String, String>> itt = it.next().entrySet().iterator();itt.hasNext();) {
				Entry<String, String> entry = itt.next();

				// DEBUG
				System.out.println(entry.getKey() + " : " + entry.getValue());
				
				String[] str = dataTrim(entry.getKey(), entry.getValue());
				map.put(entry.getKey(), str);
			}
		}
		return map;
	}

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
	
	private static List<Map<String, String>> targetDataExtra(LinkedHashMap<Integer, String> headerMap,
			LinkedHashMap<Integer, LinkedHashMap<String, String>> itemListMap, List<String> pdfData) {
		
		List<Map<String, String>> data = new ArrayList<Map<String, String>>();
		Map<String, String> map = new LinkedHashMap<String, String>();
		String strData = null;
		// Header
		for (Iterator<Entry<Integer, String>> it_head = headerMap.entrySet().iterator(); it_head.hasNext();) {
			Entry<Integer, String> entryHeaderMap = it_head.next();
			
			boolean isSearch = false;
			boolean isRepeat = false;
			String strKey = null;
			int a = 1;
			
			// Header定義がPDFデータに存在しているか
			// PDFデータ
			for (Iterator<String> it_pdf = pdfData.iterator(); it_pdf.hasNext();) {
				String strPdfdata = it_pdf.next();
				
				if (!isSearch) {
					// TODO 部分一致？前方一致？
//					if (strPdfdata.startsWith(entryHeaderMap.getValue())) {
					if (strPdfdata.indexOf(entryHeaderMap.getValue()) != -1) {
						// 存在した場合、次の行以降を検索
						isSearch = true;
					}
					continue;
				}

				if (a > 1) {
					strData = strData.concat(strPdfdata);
					a--;
					continue;
				}
				if (isRepeat) {
					map.put(strKey, strData);
					a = 1;
					isRepeat = false;
				}

				// 存在した場合、項目定義データを取り出す
				Map<String, String> itemMap = itemListMap.get(entryHeaderMap.getKey());
				for (Iterator<Entry<String, String>> it_item = itemMap.entrySet().iterator();it_item.hasNext();) {
					Entry<String, String> entryItemData = it_item.next();
					String str = entryItemData.getKey().split("\\|")[0];
					if (strPdfdata.startsWith(str) && !isRepeat) {
						if (!"1".equals(entryItemData.getValue().split("-")[1])) {
							strKey = entryItemData.getKey().replaceAll("\\|", "").replaceAll("\\s{2,}", " ").trim();
							strData = strPdfdata;
							a = Integer.parseInt(entryItemData.getValue().split("-")[1]);
							isRepeat = true;
							it_item.remove();
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
	
	private static void readPDFFile(String inputFile, List<String> pdfData) throws IOException {
		File file = new File(inputFile);
		try (PDDocument document = PDDocument.load(file)){
			PDFTextStripper s = new PDFTextStripper();
			String text = s.getText(document);
			
			// DEBUG
			System.out.println("[" + text + "]");
			
			String[] str = text.split("\\r\\n");
			for (int i=0; i < str.length; i++) {
				pdfData.add(str[i]);
			}
			document.close();
		}
	}

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
}
