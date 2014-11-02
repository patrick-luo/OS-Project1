import java.util.Arrays;


public class Procedures {
	public static int square( int x ) throws InterruptedException
	{
		//Thread.sleep(4000);
		return x * x;
	}
	
	public static int cube( int x ) throws InterruptedException
	{
		//Thread.sleep(4000);
		return x * x * x;
	}
	
	public static void sort(int[] A) throws InterruptedException{
		//Thread.sleep(4000);
		Arrays.sort(A);
	}
	
	public static int min(int[] A){
		int minVal = 0;
		for(int i = 0 ; i < A.length; i++)
			minVal = Math.min(minVal, A[i]);
		
		return minVal;
	}
	
	public static int max(int[] A){
		int maxVal = 0;
		for(int i = 0 ; i < A.length; i++)
			maxVal = Math.max(maxVal, A[i]);
		
		return maxVal;
	}
	public static int[][] multiply( int[][] A, int[][] B) throws InterruptedException
	{
		Thread.sleep(5 * 1000);
		int m = A.length, l = B.length; 
		if(m == 0 || l == 0)
			throw new IllegalArgumentException("Can't handle zero-length arrays.");
		if(l != A[0].length)
			throw new IllegalArgumentException("A[m][l] B[l1][n], l != l1");
		int n = B[0].length;
		
		int[][] C = new int[m][n];
		for(int i = 0; i < m; i++)
			for(int j = 0; j < n; j++)
				for(int k = 0; k < l; k++)
					C[i][j] += A[i][k] * B[k][j];
		
		return C;
	}
}
