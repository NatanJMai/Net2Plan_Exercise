
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
		int i = 0;
		int qtd_aux = 0;
		int pos_aux = 0;
		
		for(i = 0; i < 352; i++){
			if(qtd_aux == qtd){
				break;
			}
			
			if(frequencia[i] == false){
				qtd_aux++;
				pos_aux = i;
			}else{
				qtd_aux = 0;
			}
		}
		
		pos_aux = (pos_aux - qtd) + 1;
		System.out.println("POS_AUX " + pos_aux);
		
		for(i = pos_aux; i < qtd + pos_aux; i++){
			if(this.frequencia[i] == false){
				this.frequencia[i] = true;
			}
		}
		
		return 0;
	}
}
