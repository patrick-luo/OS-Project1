import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class Client {

	private static String programNum;
	private static String versionNum;
	private static boolean mapperChanged = false;

	public static int square(int x) throws IOException, InterruptedException, ExecutionException {
		Object[] input = new Object[1];
		input[0] = x;
		ClientStub stub = new ClientStub(mapperChanged);

		int result = (Integer) stub.doProcedure(programNum, versionNum, "1",
				input);

		System.out.println("+==========================================+");
		mapperChanged = stub.mapperIsChanged();
		return result;
	}

	public static int cube(int x) throws IOException, InterruptedException, ExecutionException {
		Object[] input = new Object[1];
		input[0] = x;
		ClientStub stub = new ClientStub(mapperChanged);

		int result = 0;
		try {
			result = (Integer) stub.doProcedure(programNum, versionNum, "2",
					input);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("+==========================================+");
		mapperChanged = stub.mapperIsChanged();
		return result;

	}

	public static int max(int[] A) throws IOException, InterruptedException, ExecutionException {
		Object[] input = new Object[1];
		input[0] = A;
		ClientStub stub = new ClientStub(mapperChanged);
		int result = (Integer) stub.doProcedure(programNum, versionNum, "3",
				input);
		System.out.println("+==========================================+");
		mapperChanged = stub.mapperIsChanged();
		return result;

	}

	public static void sort(int[] A) throws IOException, InterruptedException, ExecutionException {
		Object[] input = new Object[1];
		input[0] = A;
		ClientStub stub = new ClientStub(mapperChanged);

		int[] result = (int[]) stub.doProcedure(programNum, versionNum, "4",
				input);
		for (int i = 0; i < result.length; i++)
			A[i] = result[i];

		System.out.println("+==========================================+");
		mapperChanged = stub.mapperIsChanged();
	}

	public static void multiply(int[][] A, int[][] B, int[][] C)
			throws IOException, InterruptedException, ExecutionException {
		Object[] input = new Object[2];
		input[0] = A;
		input[1] = B;
		ClientStub stub = new ClientStub(mapperChanged);

		int[][] result = (int[][]) stub.doProcedure(programNum, versionNum,
				"5", input);
		for (int i = 0; i < result.length; i++)
			for (int j = 0; j < result[0].length; j++)
				C[i][j] = result[i][j];
		System.out.println("+==========================================+");
		mapperChanged = stub.mapperIsChanged();
	}

	public static void main(String args[]) throws Exception {
		if (args.length != 2) {
			// Suppose the client wants calculate the square or cube of a number
			System.out.println("Usage: java Client <program> <version>");
			return;
		}

		programNum = args[0];
		versionNum = args[1];
//		// ClientStub stub = new ClientStub(); // create a client stub to help
//		// the client handle the RPC
//
//		
//		int x = 9;
//		int result = square(x); System.out.println("square(" + x + ")= " +
//		result);
		
//		int y = 4; 
//		int result2 = cube(y); 
//		System.out.println("cube(" + y + ")= " + result2);
		
//		int[] A = new int[1000000];
//		for(int i = A.length - 1; i >= 0; i--)
//			A[i] = i + 1;
////		int result3 = max(A);
////		System.out.println("max(int[]{1, 2, 3, ..., 156998}) = " + result3);
////	
//		sort(A); 
//		System.out.println("sort(int[]{1000000, ..., 1}) = " + Arrays.toString(A));
		
		int[][] A = new int[1000][1000];
		int[][] B = new int[1000][1000];
		int[][] C = new int[1000][1000];
		int ma = A.length, na = A[0].length;
		int mb = B.length, nb = B[0].length;
		int cnt = 0;
		for (int i = 0; i < ma; i++)
			for (int j = 0; j < na; j++)
				A[i][j] = 1;

		for (int i = 0; i < mb; i++)
			for (int j = 0; j < nb; j++)
				B[i][j] = 1;
		
		multiply(A, B, C);
		printMatrix(C);

		// int result = stub.doProcedure(args[0], args[1], args[2],);
		// System.out.println("The result is: " + result);
		// System.out.println("The result is :" + result2);

	}

	public static void printMatrix(int[][] A) {
		for (int i = 0; i < A.length; i++) {
			for (int j = 0; j < A[i].length; j++)
				System.out.printf("%d ", A[i][j]);
			System.out.println();
		}
	}

}
