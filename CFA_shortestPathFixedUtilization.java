/*******************************************************************************
 * Copyright (c) 2013-2015 Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza - initial API and implementation
 ******************************************************************************/

import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.Constants;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Given a network topology, and the offered traffic, this algorithm first routes 
 * the traffic according to the shortest path (in number of traversed links or 
 * in number of traversed km), and then fixes the capacities so that the utilization 
 * in all the links is equal to a user-defined given value.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.2, September 2015
 */
public class CFA_shortestPathFixedUtilization implements IAlgorithm
{
	public int 				    rejeitados  = 0;
	public List <Link> 		    Links 	    = new ArrayList<Link>(); 
	public Map<Integer, Double> data_rate   = new HashMap<Integer, Double>();
	
	public Map<List<Long>, List<Long>> primary_backup_list   = new HashMap<List<Long>, List<Long>>();
	
	
	public List<List<List<Long>>> list_primary 	= new ArrayList<List<List<Long>>>();
	public List<List<List<Long>>> list_backup 	= new ArrayList<List<List<Long>>>();
	List<List<Long>> list_aux_p	= new ArrayList<List<Long>>();
	List<List<Long>> list_aux_b	= new ArrayList<List<Long>>();
	
	public Demand demand = new Demand();
	public int[][] demand_10 = demand.startDemand();

	
	public void reject(){
		rejeitados++;
//		System.out.println("Rejeitados -> " + rejeitados);
		return;
	}
	
	
	public void error(){
		throw new Net2PlanException("Problema nos enlaces!");
	}
	
	
	public List<Long> get_primary_path(int i, int j, NetPlan netPlan, String shortestPathType, Set<Long> linkIds, boolean disjoint, List<Long> primary_path){
		Map<Long, Pair<Long, Long>> linkMap = netPlan.getLinkMap();
		Map<Long, Double> linkCostMap 		= shortestPathType.equalsIgnoreCase("hops") ? DoubleUtils.ones(linkIds) : netPlan.getLinkLengthInKmMap();
		
		/* Update the netPlan object with the new routes */
		netPlan.setRoutingType(Constants.RoutingType.SOURCE_ROUTING);

		Set<Long> demandIds 		= netPlan.getDemandIds();
		Map<Long, Double> h_d 		= netPlan.getDemandOfferedTrafficMap();
		List<Long> seqLinks 		= new ArrayList<Long>();
		List<Long> linkBackup 		= new ArrayList<Long>();
		List<List<Long>> linksAux 	= new ArrayList<List<Long>>();
		
		for (long demandId : demandIds){
			if(netPlan.getDemandIngressNode(demandId) == i && netPlan.getDemandEgressNode(demandId) == j){
				/* compute the shortest path for this demand */
				long ingressNodeId  = netPlan.getDemandIngressNode(demandId);
				long egressNodeId   = netPlan.getDemandEgressNode(demandId);
				double trafficValue = h_d.get(demandId);
				
				
				seqLinks = GraphUtils.getShortestPath(linkMap, ingressNodeId, egressNodeId, linkCostMap);
				
				if(disjoint) {
					linksAux 	= GraphUtils.getTwoLinkDisjointPaths(linkMap, i, j, linkCostMap);
//					System.out.println("Links Disjuntos (LinksAux)   -> " + linksAux);
					linkBackup	= linksAux.get(0);
					
					if(linkBackup.containsAll(primary_path) && primary_path.containsAll(linkBackup)){
						seqLinks = linksAux.get(1);
					}
				}
				
				if (seqLinks.isEmpty())  continue;

				/* Add the route, no protection segments assigned to the route */
				netPlan.addRoute(demandId, trafficValue, seqLinks, null);	
			}
		}
		return seqLinks;
	}
	
	
	public void start_data_rates(){
		data_rate.put(10,    25.0);
		data_rate.put(40,    25.0);
		data_rate.put(100,   37.5);
		data_rate.put(400,   75.0);
		data_rate.put(1000, 187.5);
		return;
	}
	
	
	public boolean start_nodes(Set<Long> nodeIds){
		for(long l: nodeIds){
			Link new_link = new Link(l);
			
			this.Links.add(new_link);
		}
		
		return this.Links.isEmpty() ? false: true;
	}
	
	
	public Link get_link(Long id){
		for(Link l: Links){
			if(l.id == id){
				return l;
			}
		}
		
		return null;
	}
	
	
	public boolean run_demand(Link link_i, int rate){
		boolean retorno = true;
		
		if(! link_i.insert_array(rate)){
			retorno = false;
//			reject();
		}
		
		return retorno;
	}
	
	
	public void test_demand(){
		int i 	 	= 0;
		Link link_i = get_link((long) 1);
		
		for(i = 0;  i < 5;  i++) link_i.frequency[i] = true;
		
		if(! link_i.insert_array(350)) reject();
		
		for(i = 0; i < 30; i++){
			System.out.println("Indice " + i + " -> " + link_i.frequency[i]);
		}
	}
	
	
	public void print_links(){
		int i = 0;
		
		for(Link n: Links){
			System.out.println("------------------- Link " + n.id);
			for(i = 0; i < 352; i++){
				if(n.frequency[i] == true){
					System.out.println("Freq " + n.frequency[i] + " " + i);	
				}
			}	
			
			System.out.println();
		}
	}
	
	
	public List<Long> get_disjoint_path(int i, int j, NetPlan netPlan, String shortestPathType, Set<Long> linkIds, List<Long> primary_path){
		List<Long> backup_path = new ArrayList<Long>();
		
		backup_path = get_primary_path(i, j, netPlan, shortestPathType, linkIds, true, primary_path);
		return backup_path;
	}
	
	
	public void print_lists(List<List<List<Long>>> list){
		for(int i = 0; i < list.size(); i++) {
			System.out.println("------------- Nos "  + list.get(i).get(1).get(0) + " -> " + list.get(i).get(1).get(1));
			System.out.println("------------- Link " + list.get(i).get(0));
			System.out.println();
		}
		
	}
	
	public void print_freq(List<Long> path){
		for(Long p: path){
			Link link = get_link(p);
			
			for(int i = 0; i < 352; i++){
				if(link.frequency[i] == true){
//					System.out.println(link.frequency[i] + " " + i);	
				}
			}	
			
		}
			
	}
	
	
	public List<Long> overlaps(List<Long> bi){
		List<Integer> list_freq_i = new ArrayList<Integer>();
		List<Integer> list_freq_j = new ArrayList<Integer>();
		Link link_i = null;
		Link link_j = null;
		
		for(Long path_i: bi){
			link_i = get_link(path_i);
			
			for(int i = 0; i < 352; i++){
				if(link_i.frequency[i] == true){
					list_freq_i.add(i);
				}
			}
		}
		
		for(List<Long> bj: list_aux_b){
			if(!bj.containsAll(bi)){
			
				list_freq_j.clear();
				
				for(Long path_j: bj){
					link_j = get_link(path_j);
				
					for(int i = 0; i < 352; i++){
						if(link_j.frequency[i] == true){
							list_freq_j.add(i);
						}
					}
				}
				
				for(int i = 0; i < list_freq_i.size(); i++){
					for(int j = 0; j < list_freq_j.size(); j++){
						if(list_freq_i.get(i) == list_freq_j.get(j)){
							return bj;
						}
					}
				}
			}
		}
		return null;
	}
	
	
	public boolean isDisjoint(List<Long> pi, List<Long> pj){
		for(Long p_pi: pi){
			for(Long p_pj: pj){
				if(p_pi == p_pj)
					return false;
			}
		}
		
		return true;
	}
	
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		int i, j, rate = 0;
		
		boolean ret    = false;
		
		final int N = netPlan.getNumberOfNodes();
		final int E = netPlan.getNumberOfLinks();
		//final int D = netPlan.getNumberOfDemands();
		
		List<Long> primary_path   = new ArrayList<Long>();	
		List<Long> backup_path    = new ArrayList<Long>();
		List<Long> backup_j		  = new ArrayList<Long>();
		List<Long> primary_j	  = new ArrayList<Long>();
		List<Long> primary_i	  = new ArrayList<Long>();
		
		List<List<Long>> pathAceitos 	                = new ArrayList<List<Long>>();
		List<List<Long>> pathAceitosSemCompartilhamento = new ArrayList<List<Long>>();
		
//		Set<Long>  nodeIds		  = netPlan.getNodeIds();
		Set<Long>  linkIds 		  = netPlan.getLinkIds();
		
		if (N == 0 || E == 0)	throw new Net2PlanException("ERRO");

		/* Initialize some variables */
		final double cg = Double.parseDouble(algorithmParameters.get("cg"));
		final String shortestPathType = algorithmParameters.get("shortestPathType");
		if ((cg <= 0) || (cg > 1)) throw new Net2PlanException("Cg parameter must be positive, and below or equal to 1");
		
		if (!shortestPathType.equalsIgnoreCase("km") && !shortestPathType.equalsIgnoreCase("hops"))
			throw new Net2PlanException("Wrong shortestPathType parameter");
		
		
		
//		[[[1, 3, 4, 5], [i, j]]]
		
		/* Inicializa vetor de nos para a criacao das classes */
		if (! start_nodes(linkIds)) error();
		
		/* Inicializa a lista de taxa de dados */
		start_data_rates();
		
		/* Inicializa a matriz de demanda */
//		start_demand_array();
		
		for(i = 0; i < demand_10.length; i++){
			for(j = 0; j < demand_10.length; j++){
				if(this.demand_10[i][j] != 0){
					List<Long> 	     list_ij	= new ArrayList<Long>();
					
					list_ij.add((long) i);
					list_ij.add((long) j);
					
					primary_path = get_primary_path(i, j, netPlan, shortestPathType, linkIds, false, null);
					backup_path  = get_disjoint_path(i, j, netPlan, shortestPathType, linkIds, primary_path);
					
					primary_backup_list.put(backup_path, primary_path);
					
					list_aux_p.add(primary_path);
					list_aux_b.add(backup_path);
					
					list_primary.add(list_aux_p);
					list_backup.add(list_aux_b);
					
				}
			}
		}
		
		for(i = 0; i < demand_10.length; i++){
			for(j = 0; j < demand_10.length; j++){
				if(this.demand_10[i][j] != 0){
					
					/* Step-1 - Inicio */
					/* Pega o caminho primario entre o no origem (i) e o destino (j) */
					primary_path = get_primary_path(i, j, netPlan, shortestPathType, linkIds, false, null);
					
					if(primary_path.isEmpty()){
						reject();
					}
					else{
						System.out.println("\n---------------------------\n");
						System.out.println("Nos: " + i + " -> " + j);
						System.out.println("Link primario -> " + primary_path);	
						
						for(Long path: primary_path){
							Link link = get_link(path);
							
							rate = (int) ((data_rate.get(10) / 12.5) * demand_10[i][j]);
							
							if(run_demand(link, rate) == false){
								if(path != primary_path.get(primary_path.size() - 1)) /* Se nao for o ultimo caminho da lista de caminhos  */
									continue;
								else
									reject();
							}
						}
					}
					/* Step-1 - Fim */
					
					/* Step-2 - Inicio */
					backup_path = get_disjoint_path(i, j, netPlan, shortestPathType, linkIds, primary_path);
					
//					primary_backup_list.put(backup_path, primary_path);
					
					if(backup_path.isEmpty()){
						continue;
					}else{
						/* Step-3 */
						for(Long b_path: backup_path){
							Link link = get_link(b_path);
							rate = (int) ((data_rate.get(10) / 12.5) * demand_10[i][j]);
							ret  = run_demand(link, rate);
							
							/* Bi is not end of path list */
							if(b_path != backup_path.get(backup_path.size() - 1)){
								if(ret == true){
									/* fBi > 0. Linha 29 */
									
									backup_j = overlaps(backup_path);
									if(backup_j != null){
										
										//pi = primary_path
										//pj = primary_backup_list.get(backup_j)
										
										
										primary_i = primary_path;
										primary_j = primary_backup_list.get(backup_j);
										
										if(isDisjoint(primary_i, primary_j)) {
                                            if(link.splitting < 2) {
                                            	link.splitting++;
                                            	
                                            	if(!pathAceitos.contains(backup_path)){
                                            		pathAceitos.add(backup_path);
                                            	}
                                            	
                                            }else{
                                                continue; // Repeat Step-3 for another fBi
                                            }
                                        }
										
									}else{
				                      	if(!pathAceitosSemCompartilhamento.contains(backup_path)){
				                      		pathAceitosSemCompartilhamento.add(backup_path); // Accept backup path Bi without sharing
	                                	}
									}
									print_freq(backup_path);
								}else{
                                    continue; // Repeat Step-3 for another fBi
                                }
							}else{
								reject(); // Reject the connection
							}
							/* Step-3 - Fim */
						}
						
						System.out.println("Link backup -> " + backup_path);	
						
					}
					/* Step-2 - Fim */
					
					
				}
			}
		}
		
		System.out.println("\n---------------------------\n");
		System.out.println("Caminhos aceitos: " + pathAceitos);
//		System.out.println("Caminhos aceitos sem compartilhamento: " + pathAceitosSemCompartilhamento);
		System.out.println("Probabilidade de bloqueio (links): " + rejeitados);
		
		return "Algoritmo finalizado!";
	}

	@Override
	public String getDescription()
	{
		return "Algoritmo de Redes: Natan e Ricardo";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		List<Triple<String, String, String>> algorithmParameters = new LinkedList<Triple<String, String, String>>();
		algorithmParameters.add(Triple.of("cg", "0.6", "Fixed link utilization"));
		algorithmParameters.add(Triple.of("shortestPathType", "#select# hops km", "Criteria to compute the shortest path. Valid values: 'hops' or 'km'"));
		
		return algorithmParameters;
	}
}