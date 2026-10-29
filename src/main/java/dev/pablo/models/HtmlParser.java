package dev.pablo.models;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HtmlParser {

    private static final int MIN_CELLS = 2;
    private static final int LAST_PARSED_CELL = 8;

    public static List<DidModel> ParseDIDs(String html) {

        List<DidModel> dids = new ArrayList<>();

        if (html == null || html.isBlank()) {
            return dids;
        }

        Document doc = Jsoup.parse(html);

        // Select all 'tr' elements that conitains the class 'records_list_'.
        Elements rows = doc.select("tr[class*='records_list_']");

        for (Element row : rows) {
            // get all cell of each row.
            Elements cells = row.select("td");

            if (cells.size() < MIN_CELLS) {
                continue;
            }

            Integer id = parseId(cells.get(0).text());

            if (id == null) {
                continue;
            }

            DidModel did = new DidModel();
            did.setId(id);

            // build up a did based on positions of each sub-cell.
            for (int index = 1; index < cells.size() && index <= LAST_PARSED_CELL; index++) {
                String value = cells.get(index).text();

                switch (index) {
                    case 1:
                        did.setCallerId(value);
                        break;
                    case 2:
                        did.setDescription(value);
                        break;
                    case 3:
                        did.setCarrier(value);
                        break;
                    case 4:
                        if (!value.isBlank()) {
                            did.setActive(value.charAt(0));
                        }
                        break;
                    case 5:
                        did.setGroup(value);
                        break;
                    case 6:
                        did.setRoute(value);
                        break;
                    case 7:
                        did.setRec(value);
                        break;
                    case 8:
                        did.setModify(value);
                        break;
                }
            }

            dids.add(did);
        }

        return dids;
    }

    private static Integer parseId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
