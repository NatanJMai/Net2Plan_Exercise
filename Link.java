
public class Link {
	long id;
	int splitting;
	boolean[] frequency;
	
	public Link(long id){
		int i 	  	   = 0;
		this.id   	   = id;
		this.frequency = new boolean[352];
		this.splitting = 0;
		
		for(i = 0; i < 352; i++)
			this.frequency[i] = false;
		
	}
	
	public boolean insert_array(int qtd){
		boolean   found  = false;
		boolean[] backup = new boolean[352];
		
		int ind_aux = 0;
		int qtd_aux = 0;
		int pos_aux = 0;
		
		System.arraycopy( frequency, 0, backup, 0, frequency.length );
		
		/* Regra da continuidade */
		for(ind_aux = 0; ind_aux < 352; ind_aux++){
			if(qtd_aux == qtd){
				found = true;
				break;
			}
			
			if(frequency[ind_aux] == false){
				qtd_aux++;
				pos_aux = ind_aux;
			}else{
				qtd_aux = 0;
			}
		}
		
		pos_aux = (pos_aux - qtd) + 1;
		
		if(pos_aux < 352 && pos_aux >= 0){
			for(ind_aux = pos_aux; ind_aux < qtd + pos_aux; ind_aux++){
				if(this.frequency[ind_aux] == false){
					this.frequency[ind_aux] = true;
				}
			}	
		}
		
		
		if(found == false){
			frequency = backup;
		}
		
		return found;
	}
}
