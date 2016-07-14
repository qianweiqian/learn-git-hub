package com.android.dialer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
//BOWAY BEGIN  weiqiang.qian 20160714
//this is the add part
//BOWAY END
public class CMDExecute {

	public synchronized String run(String[] cmd, String workdirectory)
			throws IOException {
       
		String result = "";

		try {

			ProcessBuilder builder = new ProcessBuilder(cmd);

			InputStream in = null;


			if (workdirectory != null) {

				builder.directory(new File(workdirectory)); 

				builder.redirectErrorStream(true);

				Process process = builder.start();

				in = process.getInputStream();

				byte[] re = new byte[1024];

				while (in.read(re) != -1)

					result = result + new String(re);

			}

			if (in != null) {

				in.close();

			}

		} catch (Exception ex) {

			ex.printStackTrace();

		}

		return result;

	}

}
