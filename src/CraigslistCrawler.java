import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.io.Serializable;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedWriter;

import java.net.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class CraigslistCrawler {
    private static final String Craigslist_Query_URL = "https://sfbay.craigslist.org/d/apts-housing-for-rent/search/apa";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36";
    private static final String authUser = "bittiger";
    private static final String authPassword = "cs504";

    private static final int maxOutputLines = 20;

    private static final String PROXY_FILE = "proxylist_bittiger.csv";
    private static final String LOG_FILE = "crawler_log.txt";
    private static final String OUTPUT_FILE = "crawler_output.txt";

    private static List<String> proxyList;
    private static List<Listing> crawled_results = new ArrayList<Listing>();
    private static int index = 0;
    private static BufferedWriter logBFLogWriter;

    static class Listing implements Serializable{
        public String title;
        public String price;
        public String detailURL;
        public String hood;

        public Listing() {
            title = "";
            price = "";
            detailURL = "";
            hood = "";
        }
    }

    public static void main(String[] args) throws IOException {

        CraigslistCrawler.Crawl(Craigslist_Query_URL);
    }

    public static void Crawl(String url) throws IOException {

        initProxyList(PROXY_FILE);
        initLog(LOG_FILE);
        crawlListingInfo(url);
        writeResultsFile();
        logBFLogWriter.close();

    }

    public static void initProxyList(String proxy_file) {
        proxyList = new ArrayList<String>();
        try (BufferedReader br = new BufferedReader(new FileReader(proxy_file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",");
                String ip = fields[0].trim();
                proxyList.add(ip);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Authenticator.setDefault(
                new Authenticator() {
                    @Override
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                authUser, authPassword.toCharArray());
                    }
                }
        );

        System.setProperty("http.proxyUser", authUser);
        System.setProperty("http.proxyPassword", authPassword);
        System.setProperty("socksProxyPort", "61336"); // set proxy port
    }

    public static void setProxy() {
        //rotate, round robbin
        if (index == proxyList.size()) {
            index = 0;
        }
        String proxy = proxyList.get(index);
        System.setProperty("socksProxyHost", proxy); // set proxy server
        index++;
    }

    public static void initLog(String log_path) {
        try {
            File log = new File(log_path);
            // if file doesnt exists, then create it
            if (!log.exists()) {
                log.createNewFile();
            }
            FileWriter fw = new FileWriter(log.getAbsoluteFile());
            logBFLogWriter = new BufferedWriter(fw);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void crawlListingInfo(String url) {
        try {

            setProxy();
            HashMap<String,String> headers = new HashMap<String,String>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.put("Accept-Language", "en-US,en;q=0.8");
            Document doc = Jsoup.connect(url).headers(headers).userAgent(USER_AGENT).timeout(100000).get();

            Elements results = doc.select("li[data-pid]");

            if (results.size() == 0) {
                logBFLogWriter.write("0 result found");
                logBFLogWriter.newLine();
            }

            System.out.println("num of filtered results = " + results.size());

            for(Element element : results) {

                Listing house = new Listing();

                Element node1 = element.select("a[href][data-id][class]").first();
                if (node1 != null){
                    house.title = node1.text();
                    house.detailURL = node1.attr("href");
                }

                Element node2 = element.select("span[class=result-price]").first();
                if (node2 != null)
                    house.price = node2.text();

                Element node3 = element.select("span[class=result-hood]").first();
                if (node3 != null)
                    house.hood = node3.text();

                crawled_results.add(house);
            }

            System.out.println("num of returned results = " + crawled_results.size());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeResultsFile(){

        try {
            File log = new File(OUTPUT_FILE);
            // if file does not exists, then create it
            if (!log.exists()) {
                log.createNewFile();
            }
            FileWriter fw = new FileWriter(log.getAbsoluteFile());
            BufferedWriter logBFOutputWriter = new BufferedWriter(fw);

            int accumulator = 0;

            for(Listing page : crawled_results) {

                if (accumulator >= maxOutputLines)
                    break;

                String outputLine = Integer.toString(accumulator+1) + "    "
                        + page.title + "    "
                        + page.price + "    "
                        + page.detailURL + "    "
                        + page.hood;

                logBFOutputWriter.write(outputLine);
                logBFOutputWriter.newLine();

                accumulator++;
            }

            logBFOutputWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
