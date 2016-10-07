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
	public List <Node> 		    Nodes 	    = new ArrayList<Node>(); 
	public Map<Integer, Double> data_rate   = new HashMap<Integer, Double>();
	
	public int[][] demand_10 = new int[3][3];

	
	public void reject(){
		rejeitados++;
		return;
	}
	
	public void error(){
		throw new Net2PlanException("Problema nos enlaces!");
	}
	
	public List<Long> get_primary_path(Long origem, Long destino, NetPlan netPlan, String shortestPathType, Set<Long> linkIds ){
		Map<Long, Pair<Long, Long>> linkMap = netPlan.getLinkMap();
		Map<Long, Double> linkCostMap = shortestPathType.equalsIgnoreCase("hops") ? DoubleUtils.ones(linkIds) : netPlan.getLinkLengthInKmMap();
		
		/* Update the netPlan object with the new routes */
		netPlan.setRoutingType(Constants.RoutingType.SOURCE_ROUTING);

		Set<Long> demandIds = netPlan.getDemandIds();
		Map<Long, Double> h_d = netPlan.getDemandOfferedTrafficMap();
		List<Long> seqLinks = new ArrayList<Long>();
		
		for (long demandId : demandIds){
			if(netPlan.getDemandIngressNode(demandId) == origem && netPlan.getDemandEgressNode(demandId) == destino){
				/* compute the shortest path for this demand */
				long ingressNodeId = netPlan.getDemandIngressNode(demandId);
				long egressNodeId = netPlan.getDemandEgressNode(demandId);
				double trafficValue = h_d.get(demandId);
				seqLinks = GraphUtils.getShortestPath(linkMap, ingressNodeId, egressNodeId, linkCostMap);
				
				System.out.println(seqLinks);
				if (seqLinks.isEmpty())  continue;

				/* Add the route, no protection segments assigned to the route */
				netPlan.addRoute(demandId, trafficValue, seqLinks, null);	
			}
		}
		return seqLinks;
	}
	
	public void start_data_rates(){
		data_rate.put(10, 25.0);
		data_rate.put(40, 25.0);
		data_rate.put(100, 37.5);
		data_rate.put(400, 75.0);
		data_rate.put(1000, 187.5);
		return;
	}
	
	public void start_demand_array(){
		demand_10[0][0] = 0;
		demand_10[0][1] = 1;
		demand_10[0][2] = 1;
		
		demand_10[1][0] = 1;
		demand_10[1][1] = 0;
		demand_10[1][2] = 2;
		
		demand_10[2][0] = 2;
		demand_10[2][1] = 1;
		demand_10[2][2] = 0;
		
		return;
	}
	
	public boolean start_nodes(Set<Long> nodeIds){
		for(long l: nodeIds){
			Node new_node = new Node(l);
			
			this.Nodes.add(new_node);
		}
		
		return this.Nodes.isEmpty() ? false: true;
	}
	
	
	public Node get_node(int id){
		for(Node l: Nodes){
			if(l.id == id){
				return l;
			}
		}
		
		return null;
	}
	
	
	public void run_demand(){
		int i, ind, j 	 = 0;
		int rate     = 0;
		
		for(i = 0; i < 3; i++){
			for(j = 0; j < 3; j++){
				if(this.demand_10[i][j] != 0){
					Node node_i = get_node(i);
					Node node_j = get_node(j);
					
					System.out.print("No " + i + " -> " + j + "  = ");
					rate = (int) ((data_rate.get(10) / 12.5) * demand_10[i][j]);
					System.out.println(rate);
					
					node_i.insert_array(rate);
					node_j.insert_array(rate);
				}
			}
		}

		for(Node n: Nodes){
			System.out.println("------------------- No " + n.id);
			for(ind = 0; ind < 15; ind++){
				if(n.frequencia[ind] == true){
					System.out.println("Freq " + n.frequencia[ind] + " " + ind);	
				}
			}	
			
			System.out.println();
		}
		
		
	}
	
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Basic checks */
		Long origem 	= (long) 0;
		Long destino	= (long) 1;
		
		final int N = netPlan.getNumberOfNodes();
		final int E = netPlan.getNumberOfLinks();
		//final int D = netPlan.getNumberOfDemands();
		
		List<Long> primary_path   = new ArrayList<Long>();	
		Set<Long>  nodeIds		  = netPlan.getNodeIds();
		Set<Long>  linkIds 		  = netPlan.getLinkIds();
		
		if (N == 0 || E == 0)	throw new Net2PlanException("This algorithm natan a topology with links and a demand set");

		/* Initialize some variables */
		final double cg = Double.parseDouble(algorithmParameters.get("cg"));
		final String shortestPathType = algorithmParameters.get("shortestPathType");
		if ((cg <= 0) || (cg > 1)) throw new Net2PlanException("Cg parameter must be positive, and below or equal to 1");
		
		if (!shortestPathType.equalsIgnoreCase("km") && !shortestPathType.equalsIgnoreCase("hops"))
			throw new Net2PlanException("Wrong shortestPathType parameter");
		
		
		/* Pega o caminho primario entre o no origem e o destino */
		primary_path = get_primary_path(origem, destino, netPlan, shortestPathType, linkIds);
		if(primary_path.isEmpty()){
			reject();
		}
		
		/* Inicializa vetor de nos para a criacao das classes */
		if (! start_nodes(nodeIds)) error();
		
		start_data_rates();
		start_demand_array();
		run_demand();
		
		
		

		

		/* For each link, set the capacity as the one which fixes the utilization to the given value */
		Map<Long, Double> y_e = netPlan.getLinkCarriedTrafficMap();
		for (long linkId : linkIds) netPlan.setLinkCapacity(linkId, y_e.get(linkId) / cg);

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