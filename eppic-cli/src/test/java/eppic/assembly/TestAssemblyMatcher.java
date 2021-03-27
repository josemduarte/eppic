package eppic.assembly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eppic.EppicParams;
import eppic.Main;
import eppic.analysis.Utils;
import eppic.model.db.AssemblyContentDB;
import eppic.model.db.AssemblyDB;
import eppic.model.db.AssemblyScoreDB;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class TestAssemblyMatcher {

    private static final int TO_COMPARE = 10;
    private static final File OUT_DIR = new File("/Users/jose/tmp");
    private static final String LIST_FILE = "/Users/jose/Downloads/all-pdb-list";

    @Test
    public void testAgainstApi() throws IOException {
        List<String> list = new ArrayList<>(Utils.readListFile(new File(LIST_FILE)).keySet());
        File outFile = new File(OUT_DIR, "assembly_matcher.report");
        Collections.shuffle(list);

        PrintWriter pw = new PrintWriter(outFile);
        pw.printf("%4s %4s %4s %4s --- %4s %4s %4s\n", "pdb", "size", "sym", "sto", "size", "sym", "sto");

        for (int i=0; i<TO_COMPARE; i++) {
            String pdbId = list.get(i);

            AssemblyContentDB fromRest = getFromRest(pdbId);
            if (fromRest == null)
                continue;

            AssemblyDB matched = runAssemblyMatching(pdbId);

            if (matched == null) {
                System.err.println("Could not find assembly match for " + pdbId);
                continue;
            }

            AssemblyContentDB matchedAsContDb = matched.getAssemblyContents().get(0);

            pw.printf("%4s %s --- %s\n", pdbId, formatAssCont(matchedAsContDb), formatAssCont(fromRest));

        }
        pw.close();

    }

    private String formatAssCont(AssemblyContentDB asContDb) {
        return String.format("%4s %4s %4s", asContDb.getMmSize(), asContDb.getSymmetry(), asContDb.getStoichiometry());
    }

    private AssemblyDB runAssemblyMatching(String pdbId) {
        Main m = new Main();
        EppicParams params = eppic.Utils.generateEppicParams(pdbId, OUT_DIR);
        m.run(params);
        for (AssemblyDB adb : m.getDataModelAdaptor().getPdbInfo().getValidAssemblies()) {
            for (AssemblyScoreDB asdb : adb.getAssemblyScores()) {
                if (asdb.getMethod().equals("pdb1")) {
                    return adb;
                }
            }
        }
        return null;
    }

    private AssemblyContentDB getFromRest(String pdbId) throws IOException {
        URL url = new URL("http://www.eppic-web.org/rest/api/v3/job/assemblyByPdbId/"+pdbId+"/1");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        int code = connection.getResponseCode();
        if (code != 200) {
            System.err.println("No REST results available for "+pdbId);
            return null;
        }

        InputStream input = connection.getInputStream();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode obj = mapper.readValue(input, JsonNode.class);
        JsonNode contentNode = obj.get("assemblyContents").get("assemblyContent");
        if (!contentNode.isArray()) {
            System.err.println("assemblyContent node is not an array");
            return null;
        }
        AssemblyContentDB ascdb = null;
        for (JsonNode sc : contentNode) {
            if (ascdb != null) {
                System.err.println("More than 1 assembly content");
                return null;
            }
            ascdb = mapper.readValue(sc.toString(), AssemblyContentDB.class);
        }
        connection.disconnect();
        return ascdb;
    }
}
