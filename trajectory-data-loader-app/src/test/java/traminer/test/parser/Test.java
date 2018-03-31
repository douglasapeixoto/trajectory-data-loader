package traminer.test.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import traminer.io.IOService;
import traminer.parser.ParserException;
import traminer.parser.analyzer.Keywords;
import traminer.util.DateUtils;

public class Test {
/*
	public static void main(String[] args) {
		/*String dateTimeDescr = "DATETIME(\"alegria de viver\")";
		int s = dateTimeDescr.indexOf("\"");
		int e = dateTimeDescr.indexOf("\"",10);
		String datePattern = dateTimeDescr.substring(s+1, e);
		System.out.println(s);
		System.err.println(datePattern);

		String date = "3/2/2009 10:04:44 AM";
		Date newDate = DateUtils.parseDate(date, "M/d/yyyy H:mm:ss a");
			
			System.out.println(newDate.getTime());
		System.out.println("FINISHED!!!");
		
		
	}
*/	

	public static void main(String[] args) throws IOException {
		List<String> fileLines = IOService.readFile("E:/data/gc-roads.geojson");
		List<String> poiList = new ArrayList<>();
		for (String road : fileLines) {
			//if (poi.contains("\"amenity\": null")) continue;
			//if (poi.contains("\"historic\": null")) continue;
			//if (poi.contains("\"leisure\": null")) continue;
			//if (poi.contains("\"natural\": null")) continue;
			//if (poi.contains("\"shop\": null")) continue;
			//if (poi.contains("\"sport\": null")) continue;
			//if (poi.contains("\"tourism\": null")) continue;
			//if (poi.contains("\"poi\": null")) continue;
			
			/*
			poi = poi.replace("\"access\": null, ", "");
			poi = poi.replace("\"aerialway\": null, ", "");
			poi = poi.replace("\"aeroway\": null, ", "");
			poi = poi.replace("\"area\": null, ", "");
			poi = poi.replace("\"barrier\": null, ", "");
			poi = poi.replace("\"bicycle\": null, ", "");
			poi = poi.replace("\"brand\": null, ", "");
			poi = poi.replace("\"bridge\": null, ", "");
			poi = poi.replace("\"boundary\": null, ", "");
			poi = poi.replace("\"building\": null, ", "");
			poi = poi.replace("\"capital\": null, ", "");
			poi = poi.replace("\"covered\": null, ", "");
			poi = poi.replace("\"culvert\": null, ", "");
			poi = poi.replace("\"cutting\": null, ", "");
			poi = poi.replace("\"disused\": null, ", "");
			poi = poi.replace("\"ele\": null, ", "");
			poi = poi.replace("\"embankment\": null, ", "");
			poi = poi.replace("\"foot\": null, ", "");
			poi = poi.replace("\"harbour\": null, ", "");
			poi = poi.replace("\"highway\": null, ", "");
			poi = poi.replace("\"horse\": null, ", "");
			poi = poi.replace("\"junction\": null, ", "");
			poi = poi.replace("\"landuse\": null, ", "");
			poi = poi.replace("\"layer\": null, ", "");
			poi = poi.replace("\"lock\": null, ", "");
			poi = poi.replace("\"man_made\": null, ", "");
			poi = poi.replace("\"military\": null, ", "");
			poi = poi.replace("\"motorcar\": null, ", "");
			poi = poi.replace("\"oneway\": null, ", "");
			poi = poi.replace("\"operator\": null, ", "");
			poi = poi.replace("\"population\": null, ", "");
			poi = poi.replace("\"power\": null, ", "");
			poi = poi.replace("\"place\": null, ", "");
			poi = poi.replace("\"railway\": null, ", "");
			poi = poi.replace("\"ref\": null, ", "");
			poi = poi.replace("\"religion\": null, ", "");
			poi = poi.replace("\"route\": null, ", "");
			poi = poi.replace("\"service\": null, ", "");
			poi = poi.replace("\"surface\": null, ", "");
			poi = poi.replace("\"toll\": null, ", "");
			poi = poi.replace("\"tower:type\": null, ", "");
			poi = poi.replace("\"tunnel\": null, ", "");
			poi = poi.replace("\"water\": null, ", "");
			poi = poi.replace("\"waterway\": null, ", "");
			poi = poi.replace("\"wetland\": null, ", "");
			poi = poi.replace("\"width\": null, ", "");
			poi = poi.replace("\"wood\": null, ", "");
			poi = poi.replace("\"z_order\": null, ", "");
			
			poi = poi.replace("\"poi\": null", "\"poi\": \"amenity\"");
			*/
			
			/* ROAD
			int i = road.indexOf("\"ref\":");
			int j = road.indexOf("\"class\":");
			road = road.substring(0, i) + 
				   road.substring(j);
			*/	   
			
			//int i = road.indexOf("}") - 2;
			String s = genRandomData();
			road = road.replaceFirst(" \\}", s);
				
			
/*
			int i = road.indexOf("\"class\":");
			int j = road.indexOf("}");
			road = road.substring(0, i) + 
				   road.substring(j);
*/			
			poiList.add(road); 
		}
		IOService.writeFile(poiList, "E:/data/", "out_roads.geojson");
		
		System.out.println("FINISHED!!!");
	}

	private static String genRandomData() {
		int human  = (int)(70 * Math.random());
		int nature = (int)((100-human) * Math.random());
		int building = (int)((100-human-nature) * Math.random());
		int heavytraffic = (int)((100-human-nature-building) * Math.random());
		int lighttraffic = (100-human-nature-building-heavytraffic);

		int max = human;
		String label = "human";
		if (nature > max) {
			max = nature;
			label = "nature";
		}
		if (building > max) {
			max = building;
			label = "building";
		}
		if (heavytraffic > max) {
			max = heavytraffic;
			label = "heavytraffic";
		}
		if (lighttraffic > max) {
			max = lighttraffic;
			label = "lighttraffic";
		}
	
		String s = " \"human\": " + human
				 + ", \"nature\": " + nature 
				 + ", \"building\": " + building
				 + ", \"heavytraffic\": " + heavytraffic
				 + ", \"lighttraffic\": " + lighttraffic 
				 + ", \"class\": \"" + label + "\"" 
				 + " }";		
		
		return s;
	}

}
