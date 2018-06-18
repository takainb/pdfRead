package pdfRead;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;


public class pdfBox1 {
	
	public static final String INPUT_PATH = "C:\\test\\pdf\\";
	public static final String OUTPUT_PATH = "C:\\test\\result\\";
	public static final String DEFINE_FILE = "C:\\test\\item\\item.txt";
	public static final String PDF = ".pdf";
	public static final String CSV = ".csv";
	public static final String AST = "(*)";
	
	public static void main(String...args) throws IOException {
		
		// 引数Check
		if (args.length != 1) {
			System.out.println("引数エラーです");
		} else {
			String filename = args[0].replace(PDF, "");
			String inputFile = INPUT_PATH + args[0];
			String outputFile = OUTPUT_PATH + filename + CSV;
			
			LinkedHashMap<Integer, LinkedHashMap<String, String>> itemListMap = new LinkedHashMap<Integer, LinkedHashMap<String, String>>();
			List<String> pdfData = new ArrayList<String>();
			
			// 項目定義の読み込み
			readItem(DEFINE_FILE, itemListMap);
			// PDF読み取り
			readPDFFile(inputFile, pdfData);
			// 対象データ抜き取り
			List<Map<String, String>> dataMap = targetDataExtra(itemListMap, pdfData);
			
			// データ加工
			Map<String, String[]> dataList = dataProcessing(dataMap);
			
			// 結果出力
			outputResult(dataList, outputFile);
		}
	}

	private static void outputResult(Map<String, String[]> dataList, String outputFile) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		FileWriter fw = new FileWriter(outputFile,false);
		PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
		
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
		// TODO 自動生成されたメソッド・スタブ
		Map<String, String[]> map = new LinkedHashMap<String, String[]>();
		
		for (Iterator<Map<String, String>> it = dataMap.iterator(); it.hasNext();) {
			for (Iterator<Entry<String, String>> itt = it.next().entrySet().iterator();itt.hasNext();) {
				Entry<String, String> entry = itt.next();
				System.out.println(entry.getKey() + " : " + entry.getValue());
				
				String[] str = dataTrim(entry.getKey(), entry.getValue());
				map.put(entry.getKey(), str);
			}
		}
		return map;
	}

	private static String[] dataTrim(String key, String value) {
		// TODO 自動生成されたメソッド・スタブ
		String line = null;
		String[] data = null;
		String str = key + AST;
		
		line = value.replace(str, "");
		line = line.replace(key, "");
		
		line = line.trim();
		data = line.split(" ");
		
		return data;
	}

	private static List<Map<String, String>> targetDataExtra(LinkedHashMap<Integer, LinkedHashMap<String, String>> itemListMap,
			List<String> pdfData) {
		// TODO 自動生成されたメソッド・スタブ
		List<Map<String, String>> data = new ArrayList<Map<String, String>>();
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (Iterator<String> it1 = pdfData.iterator(); it1.hasNext();) {
			String strPdfdata = it1.next();
			for (Iterator<Entry<Integer, LinkedHashMap<String, String>>> it2 = itemListMap.entrySet().iterator(); it2
					.hasNext();) {
				Entry<Integer, LinkedHashMap<String, String>> entryItemMap = it2.next();
				
				for(Iterator<Entry<String, String>> it3 = entryItemMap.getValue().entrySet().iterator(); it3.hasNext();) {
					Entry<String, String> entryItemData = it3.next();
					
					if(strPdfdata.startsWith(entryItemData.getKey())) {
						if ("S".equals(entryItemData.getValue())) {
							int a = entryItemMap.getKey();
						} else {
							map.put(entryItemData.getKey(), strPdfdata);
							it3.remove();
							
							if("E".equals(entryItemData.getValue())) {
								data.add(map);
								return data;
							}
						}
					}
				}
			}
		}
		return data;
	}

	private static void readPDFFile(String inputFile, List<String> pdfData) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		File file = new File(inputFile);
		try (PDDocument document = PDDocument.load(file)){
			PDFTextStripper s = new PDFTextStripper();
			String text = s.getText(document);
//			System.out.println("[" + text + "]");
			
			String[] str = text.split("\r\n");
			for (int i=0; i < str.length; i++) {
				pdfData.add(str[i]);
			}
			
			document.close();
		}
	}

	private static void readItem(String defineFile, LinkedHashMap<Integer, LinkedHashMap<String, String>> map2)
			throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		File file = new File(defineFile);
		String line = null;
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(file));
		
		while((line = br.readLine()) != null) {
			String[] str = line.split(",");
			
			int a = Integer.parseInt(str[0]);
			map.put(str[2], str[1]);
			
			if("E".equals(str[1])) {
				map2.put(a, map);
				map = null;
			}
		}
		br.close();
	}

}
