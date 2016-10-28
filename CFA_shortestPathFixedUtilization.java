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
	
	public List<List<List<Long>>> list_primary 	= new ArrayList<List<List<Long>>>();
	public List<List<List<Long>>> list_backup 	= new ArrayList<List<List<Long>>>();
	List<List<Long>> list_aux_p	= new ArrayList<List<Long>>();
	List<List<Long>> list_aux_b	= new ArrayList<List<Long>>();
	
	public int[][] demand_10 = new int[4][4];

	
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
	
	
	public void start_demand_array(){
		demand_10[0][0] = 0;
		demand_10[0][1] = 1;
		demand_10[0][2] = 1;
		demand_10[0][3] = 180;
		
		demand_10[1][0] = 1;
		demand_10[1][1] = 0;
		demand_10[1][2] = 2;
		demand_10[1][3] = 0;
		
		demand_10[2][0] = 2;
		demand_10[2][1] = 1;
		demand_10[2][2] = 0;
		demand_10[2][3] = 0;
		
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
		
//		System.out.print("No " + i + " -> " + j + "  = ");
//		System.out.println(rate);
		
		if(! link_i.insert_array(rate)){
			retorno = false;
			reject();
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
					System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAA " + link.frequency[i] + " " + i);	
				}
			}	
			
		}
			
	}
	
	
	public boolean overlaps(List<Long> bi){
		List<Integer> list_freq_i = new ArrayList<Integer>();
		List<Integer> list_freq_j = new ArrayList<Integer>();
		
		for(Long path_i: bi){
			Link link_i = get_link(path_i);
			
			for(int i = 0; i < 352; i++){
				if(link_i.frequency[i] == true){
					list_freq_i.add(i);
				}
			}
		}
		
		for(List<Long> bj: list_aux_b){
			list_freq_j.clear();
			
			for(Long path_j: bj){
				Link link_j = get_link(path_j);
			
				for(int i = 0; i < 352; i++){
					if(link_j.frequency[i] == true){
						list_freq_j.add(i);
					}
				}
			}
			
			for(int i = 0; i < list_freq_i.size(); i++){
				for(int j = 0; j < list_freq_j.size(); j++){
					if(list_freq_i.get(i) == list_freq_j.get(j)){
						return true;
					}
				}
			}
		}
		
		
		
		return false;
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
//		Set<Long>  nodeIds		  = netPlan.getNodeIds();
		Set<Long>  linkIds 		  = netPlan.getLinkIds();
		
		if (N == 0 || E == 0)	throw new Net2PlanException("This algorithm natan a topology with links and a demand set");

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
		start_demand_array();
		
		for(i = 0; i < demand_10.length; i++){
			for(j = 0; j < demand_10.length; j++){
				if(this.demand_10[i][j] != 0){
					List<Long> 	     list_ij	= new ArrayList<Long>();
					
					list_ij.add((long) i);
					list_ij.add((long) j);
					
					primary_path = get_primary_path(i, j, netPlan, shortestPathType, linkIds, false, null);
					backup_path  = get_disjoint_path(i, j, netPlan, shortestPathType, linkIds, primary_path);
					
					list_aux_p.add(primary_path);
					list_aux_b.add(backup_path);
					
					
					list_aux_p.add(list_ij);
					list_aux_b.add(list_ij);
					
					list_primary.add(list_aux_p);
					list_backup.add(list_aux_b);
				}
			}
		}
		
		System.out.println("Primario: ");
		print_lists(list_primary);
		
		
		System.out.println("Backup: ");
		print_lists(list_backup);
		
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
								if(path != primary_path.get(primary_path.size() - 1)) /* Se nao for o ultimo elemento da lista de caminhos  */
									continue;
								else
									reject();
							}
						}
					}
					/* Step-1 - Fim */
					
					/* Step-2 - Inicio */
					backup_path = get_disjoint_path(i, j, netPlan, shortestPathType, linkIds, primary_path);
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
									if(overlaps(backup_path)){
										System.out.println("\nAEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE\n");
									}
									print_freq(backup_path);
								}
							}
						}
						
						System.out.println("Link backup -> " + backup_path);	
						
					}
					/* Step-2 - Fim */
					
					
				}
			}
		}
		
		
		print_links();

		
		

		

		/* For each link, set the capacity as the one which fixes the utilization to the given value */
//		Map<Long, Double> y_e = netPlan.getLinkCarriedTrafficMap();
//		for (long linkId : linkIds) netPlan.setLinkCapacity(linkId, y_e.get(linkId) / cg);

		return "Oksss!";
	}

	@Override
	public String getDescription()
	{
		return "Algoritmo de Redes: Eliton, Natan e Ricardo";
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