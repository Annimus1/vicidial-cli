package dev.pablo.models;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HtmlParser {
    public static List<DidModel> ParseDIDs(String html) {

        List<DidModel> dids = new ArrayList<>();
        Document doc = Jsoup.parse(html);

        // Select all 'tr' elements that conitains the class 'records_list_'.
        Elements rows = doc.select("tr[class*='records_list_']");

        for (Element row : rows) {
            // get all cell of each row.
            Elements cells = row.select("td");
            DidModel did = new DidModel();

            for (int index = 0; index < cells.size(); index++) {
                // build up a did based on positions of each sub-cell.
                switch (index) {
                    case 0:
                        did.setId(Integer.parseInt(cells.get(index).text()));
                        break;
                    case 1:
                        did.setCallerId(cells.get(index).text());
                        break;
                    case 2:
                        did.setDescription(cells.get(index).text());
                        break;
                    case 3:
                        did.setCarrier(cells.get(index).text());
                        break;
                    case 4:
                        did.setActive(cells.get(index).text().charAt(0));
                        break;
                    case 5:
                        did.setGroup(cells.get(index).text());
                        break;
                    case 6:
                        did.setRoute(cells.get(index).text());
                        break;
                    case 7:
                        did.setRec(cells.get(index).text());
                        break;
                    case 8:
                        did.setModify(cells.get(index).text());
                        dids.add(did);
                        break;
                }
            }

        }

        return dids;
    }
}
