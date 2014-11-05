package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
//	public static void main(String[] args) throws IOException {
//		// TODO Auto-generated method stub
//		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream("out"), "UTF-8"));
//		String line = "";
//		while((line = r.readLine()) != null){
//			if(line.contains("server workload"))
//				System.out.println(line);
//		}
//	}
	
//	public static void main(String[] args) throws IOException {
//		byte[] receiveData = new byte[1024];
//		String s = new String(receiveData).trim();
//		System.out.println(s.length() == 0);
//	}
	
//	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
//		// TODO Auto-generated method stub
//		ExecutorService executor = Executors.newSingleThreadExecutor();
//		
//		int timeout = 1;
//		while (true) {
//			Future<String> future = executor.submit(new Task());
//			try {
//				System.out.println("Started..");
//				Object result = future.get(timeout, TimeUnit.SECONDS);
//				System.out.println("finished..." + result);
//				executor.shutdownNow();
//				return;
//			} catch (TimeoutException e) {
//				timeout = 10;
//				System.out.println(future.cancel(true));
//			//	future = executor.submit(new Task2());
//				System.out.println("Timeout reached!!! retransmit!");
//			}
//		}
//	}
	
//	static class Task implements Callable<String>{
//		public String call() throws Exception{
//			System.out.println("yu qiao");
//			Thread.sleep(2000);
//			return "helllo";
//		}
//	}
//	
//	static class Task2 implements Callable<String>{
//		public String call() throws Exception{
//			Thread.sleep(2000);
//			return "yuqiao";
//		}
//	}

}
