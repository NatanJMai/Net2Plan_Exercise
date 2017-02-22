import java.util.Random;

public class Demand {
	public int[][] demand_10 = new int[13][13];
	
	public int[][] startDemand(){
//		  demand_10[0][0] = 0;
//        demand_10[0][1] = 1;
//        demand_10[0][2] = 1;
//        demand_10[0][3] = 180;
//         
//        demand_10[1][0] = 1;
//        demand_10[1][1] = 0;
//        demand_10[1][2] = 2;
//        demand_10[1][3] = 0;
//         
//        demand_10[2][0] = 2;
//        demand_10[2][1] = 1;
//        demand_10[2][2] = 0;
//        demand_10[2][3] = 0;
         
		int i, j;
		Random randomGenerator = new Random();
		
		for(i = 0; i < 13; i++){
			for(j = 0; j < 13; j++){
				if(i != j){
					demand_10[i][j] = randomGenerator.nextInt(180);
				}
//				System.out.println(i + "  " + j + ": " + demand_10[i][j]); 
			}
		}
		
		return this.demand_10;
	}
}
