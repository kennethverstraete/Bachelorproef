package Gurobi;
import java.io.IOException;
import java.util.ArrayList;

import datareader.Datareader;

import gurobi.*;

public class TUP {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		try {		
			// ----------------------------------
			// ---------- KIES DATASET ----------
			// ----------------------------------
//			String dataset = "4"; // Opl: 5176
//			String dataset = "6"; // Opl: 14077
//			String dataset = "6a"; // Opl: 15457
//			String dataset = "6b"; // Opl: 16716
//			String dataset = "6c"; // Opl: 14396
//			String dataset = "8"; // Opl: 34311
//			String dataset = "8a"; // Opl: 31490  
//			String dataset = "8b"; // Opl: 32731 
//			String dataset = "8c"; // Opl: 29879 
//			String dataset = "10"; // Opl: 48942
			
			Datareader dr = new Datareader();
			dr.getData("D:\\Dropbox\\School\\bachelorproef\\datasets\\umps"+dataset+".txt");
			int[][] dist = dr.getDist();
			int[][] opp = dr.getOpp();
			double n = dist.length/2;
			double amountTeams = dist[0].length;
			double amountSlots = 2*amountTeams-2;
						
			// Parameters
			double d1 = 0;
			double d2 = 0;
			double n1 = n-d1-1;
			double n2 = Math.floor(n/2)-d2-1;
			
			// Model
			GRBEnv env = new GRBEnv();
			GRBModel model = new GRBModel(env);
			model.set(GRB.StringAttr.ModelName, "TUP");
			
			// Create variables
			GRBVar[][][] x = new GRBVar[(int) amountTeams][(int) amountSlots][(int) n];
			for(int i=0; i<amountTeams;++i){
				for(int u=0; u<n; ++u) {
					for(int s=0; s<amountSlots; ++s) {
						x[i][s][u] = 
								model.addVar(0, 1, 1,GRB.BINARY, "x"+(i+1)+(s+1)+(u+1));
					}
				}
			}
			
			GRBVar[][][][] z = new GRBVar[(int) amountTeams][(int) amountTeams][(int) amountSlots-1][(int) n];
			for(int i=0; i<amountTeams;++i){
				for(int j=0; j<amountTeams; ++j) {
					for(int u=0; u<n; ++u) {
						for(int s=0; s<amountSlots-1; ++s) {
							z[i][j][s][u] = 
									model.addVar(0, 1, dist[i][j],GRB.BINARY, "z"+(i+1)+(j+1)+(s+1)+(u+1));
						}
					}
				}
			}
			
			// Update model to integrate new variables
			model.update();
			
			// Set objective:
			GRBLinExpr expr = new GRBLinExpr();
			for(int i=0; i<amountTeams; ++i) {
				for(int j=0; j<amountTeams; ++j) {
					for(int u=0; u<n; ++u) {
						for(int s=0; s<amountSlots-1; ++s) {
							expr.addTerm(dist[i][j],z[i][j][s][u]);
						}
					}
				}
			}
			model.setObjective(expr, GRB.MINIMIZE);
									
			// Constraints
			// 2 (works)
			for(int i=0; i<amountTeams;++i){
				for(int s=0; s<amountSlots; ++s) {
					if(opp[s][i] > 0 ) {
						GRBLinExpr d1tot = new GRBLinExpr();
						for(int u=0; u<n; ++u) {
								d1tot.addTerm(1.0,x[i][s][u]);
						}
						model.addConstr(d1tot, GRB.EQUAL, 1, "B1_x"+i+s);
					}
				}
			}
			
			// 3 (works)
			for(int s=0; s<amountSlots;++s){
				for(int u=0; u<n; ++u) {
					GRBLinExpr d2tot = new GRBLinExpr();
					for(int i=0; i<amountTeams; ++i) {
						if(opp[s][i] > 0) {
							d2tot.addTerm(1.0,x[i][s][u]);
						}
					}
					model.addConstr(d2tot, GRB.EQUAL, 1, "B2_x"+s+u);
				}
			}
			
			// 4 (works)
			for(int i=0; i<amountTeams;++i){
				for(int u=0; u<n; ++u) {
					GRBLinExpr d3tot = new GRBLinExpr();
					for(int s=0; s<amountSlots; ++s) {
						if(opp[s][i] > 0) {
							d3tot.addTerm(1.0,x[i][s][u]);
						}
					}
					model.addConstr(d3tot, GRB.GREATER_EQUAL, 1, "B3_x"+i+u);
				}
			}
			
			// 5 (works)
			for(int i=0; i<amountTeams;++i){
				for(int u=0; u<n; ++u) {
					for(int s=0; s<amountSlots-n1; ++s) {
						GRBLinExpr d4tot = new GRBLinExpr();
						for(int c=s; c<=s+n1;++c) {
							System.out.println("i: "+i+"  c: "+c+" u: "+u);
							d4tot.addTerm(1.0,x[i][c][u]);
						}
						System.out.println("-----------");
						model.addConstr(d4tot, GRB.LESS_EQUAL, 1, "B4_x"+i+s+u);
					}
				}
			}
			
			// 6 (works)
			for(int i=0; i<amountTeams;++i){
				for(int u=0; u<n; ++u) {
					for(int s=0; s<amountSlots-n2; ++s) {
						GRBLinExpr d5tot = new GRBLinExpr();
						for(int c=s; c<=s+n2;++c) {
							d5tot.addTerm(1.0,x[i][c][u]);
							for(int j=0; j<amountTeams; ++j) {
								System.out.println("i: "+i+" u: "+u+" s: "+s+" c: "+c+" j: "+j);
								if(opp[c][j] == i+1) {
									System.out.println("team "+(j+1)+" speelt op slot "+(c+1)+" tegen "+i);
									d5tot.addTerm(1.0,x[j][c][u]);
								}
							}
						}
						System.out.println("-----------");
						model.addConstr(d5tot, GRB.LESS_EQUAL, 1, "B5_x"+i+s+u);
					}
				}
			}
			
			// 7
			for(int i=0; i<amountTeams; ++i) {
				for(int j=0; j<amountTeams; ++j) {
					for(int u=0; u<n; ++u) {
						for(int s=0; s<amountSlots-1; ++s) {
							GRBLinExpr d6tot = new GRBLinExpr();
							d6tot.addTerm(1.0, x[i][s][u]);
							d6tot.addTerm(1.0, x[j][s+1][u]);
							d6tot.addTerm(-1.0, z[i][j][s][u]);
							model.addConstr(d6tot, GRB.LESS_EQUAL, 1, "xisc6"+i+j+u+s);
						}
					}
				}
			}
			
			// Solve
			model.optimize();
			printSolution(model,x,z);

			
		} catch (GRBException e) {
			System.out.println("Error code: " + e.getErrorCode() + ". " +
								e.getMessage());
			
		}
	}
	
	private static void printSolution(GRBModel model, GRBVar[][][] x,
            GRBVar[][][][] z) throws GRBException {
		if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
			System.out.println("\nCost: " + model.get(GRB.DoubleAttr.ObjVal));
			
			// Print waardes voor de x'en
//			System.out.println("\nX:");
//			for (int i = 0; i < x.length; ++i) {
//				for(int s = 0; s<x[0].length; ++s) {
//					for(int u = 0; u<x[0][0].length; ++u) {
//						//if (x[i][s][c].get(GRB.DoubleAttr.X) > 0.0001) {
//							System.out.println(x[i][s][u].get(GRB.StringAttr.VarName) + " " +
//									x[i][s][u].get(GRB.DoubleAttr.X));
//						//}
//					}
//				}
//			}
			
			// Print waardes voor alle z
//			System.out.println("\nZ:");
//			for (int i = 0; i < z.length; ++i) {
//				for(int j = 0; j< z[0].length; ++j) {
//					for(int u=0; u<z[0][0][0].length; ++u) {
//						for(int s= 0; s<z[0][0].length-1; ++s){
//							System.out.println(z[i][j][s][u].get(GRB.StringAttr.VarName) + " " +
//									z[i][j][s][u].get(GRB.DoubleAttr.X));
//						}
//					}
//				}
//			}
		} else {
			System.out.println("No solution");
		}
		}		
	}
