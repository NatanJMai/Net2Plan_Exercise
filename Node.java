
public class Node {
	long id;
	boolean[] frequencia;
	
	public Node(long id){
		int i = 0;
		this.id   = id;
		this.frequencia = new boolean[352];
		
		for(i = 0; i < 352; i++)
			this.frequencia[i] = false;
		
	}
	
	public int insert_array(int qtd){
		int i = 0, p = 0;
		
		for(i = 0; i < 352; i++){
			if(frequencia[i] == false){
				p = i;
				break;
			}
		}
		
		for(i = p; i < qtd + p; i++){
			if(this.frequencia[i] == false){
				this.frequencia[i] = true;
			}
		}
		
		return 0;
	}
}
