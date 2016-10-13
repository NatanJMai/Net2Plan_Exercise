
public class Link {
	long id;
	boolean[] frequencia;
	
	public Link(long id){
		int i = 0;
		this.id   = id;
		this.frequencia = new boolean[352];
		
		for(i = 0; i < 352; i++)
			this.frequencia[i] = false;
		
	}
	
	public boolean insert_array(int qtd){
		boolean   found  = false;
		boolean[] backup = new boolean[352];
		
		int ind_aux = 0;
		int qtd_aux = 0;
		int pos_aux = 0;
		
		System.arraycopy( frequencia, 0, backup, 0, frequencia.length );
		
		/* Regra da continuidade */
		for(ind_aux = 0; ind_aux < 352; ind_aux++){
			if(qtd_aux == qtd){
				found = true;
				break;
			}
			
			if(frequencia[ind_aux] == false){
				qtd_aux++;
				pos_aux = ind_aux;
			}else{
				qtd_aux = 0;
			}
		}
		
		pos_aux = (pos_aux - qtd) + 1;
		
		if(pos_aux < 352 && pos_aux >= 0){
			for(ind_aux = pos_aux; ind_aux < qtd + pos_aux; ind_aux++){
				if(this.frequencia[ind_aux] == false){
					this.frequencia[ind_aux] = true;
				}
			}	
		}
		
		
		if(found == false){
			frequencia = backup;
		}
		
		return found;
	}
}
