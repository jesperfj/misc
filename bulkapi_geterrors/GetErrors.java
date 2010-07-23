/**
 *
 *
 */

import java.io.File;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.force.cloudloader.CSVReader;
import com.sforce.async.BatchInfo;
import com.sforce.async.BatchStateEnum;
import com.sforce.async.RestConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectorConfig;

public class GetErrors {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		try {
			String username = args[0];
			String password = args[1];
			String jobId = args[2];

			ConnectorConfig config = new ConnectorConfig();
			config.setAuthEndpoint("https://login.salesforce.com/services/Soap/u/19");
			config.setUsername(username);
			config.setPassword(password);

			PartnerConnection p = new PartnerConnection(config);

			String restEndpoint = config.getServiceEndpoint().substring(0, config.getServiceEndpoint().indexOf("Soap"))
					+ "async/19";
			System.out.println(restEndpoint);
			config.setRestEndpoint(restEndpoint);

			RestConnection rc = new RestConnection(config);

			BatchInfo[] bis = rc.getBatchInfoList(jobId).getBatchInfo();

			FileWriter out = new FileWriter(new File("errors.csv"));

			boolean headerWritten = false;

			for (BatchInfo b : bis) {
				if (b.getState().equals(BatchStateEnum.Completed) && b.getNumberRecordsFailed() > 0) {

					URL resultURL = new URL(restEndpoint + "/job/" + jobId + "/batch/" + b.getId() + "/result");
					URL requestURL = new URL(restEndpoint + "/job/" + jobId + "/batch/" + b.getId() + "/request");
					HttpURLConnection resultC = (HttpURLConnection) resultURL.openConnection();
					HttpURLConnection requestC = (HttpURLConnection) requestURL.openConnection();
					resultC.setRequestMethod("GET");
					requestC.setRequestMethod("GET");
					resultC.addRequestProperty("X-SFDC-Session", config.getSessionId());
					requestC.addRequestProperty("X-SFDC-Session", config.getSessionId());

					CSVReader resultR = new CSVReader(resultC.getInputStream());
					CSVReader requestR = new CSVReader(requestC.getInputStream());

					ArrayList<String> resultHeader = resultR.nextRecord();
					ArrayList<String> requestHeader = requestR.nextRecord();

					while (true) {
						ArrayList<String> resultRow = resultR.nextRecord();
						ArrayList<String> requestRow = requestR.nextRecord();
						if (resultRow == null) {
							if (requestRow != null) {
								System.out.println("Result list finish before request list in batch " + b.getId());
							}
							break;
						}
						if (resultRow.get(1).equals("false")) {
							if (!headerWritten) {
								for (String name : requestHeader) {
									out.write("\"" + name.replace("\"", "\"\"") + "\",");
								}
								out.write("\"errors\"\n");
								headerWritten = true;
							}
							for (String field : requestRow) {
								if (field == null) {
									out.write(",");
								} else {
									out.write("\"" + field.replace("\"", "\"\"") + "\",");
								}
							}
							if (resultRow.get(3) != null) {
								out.write("\"" + resultRow.get(3).replace("\"", "\"\"") + "\"\n");
							} else {
								out.write("NO ERROR MESSAGE\n");
							}
						}
					}
				}
			}
			out.flush();
			out.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
