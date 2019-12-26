package rbmc;

import bots.TrainingBot;
import game.BotGame;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import rbmc.PyBot;

public class RBMC {
	public static char[][] board = new char[8][8];
	public static ArrayList<TrainingBot> bots = new ArrayList<>();
	public static int botCount = 40, generations = 100000;
	public static int total, count; // Amount of white wins, Number of total victories (not ties)
	public static boolean readGenes = true;
	public static boolean human = false;
	public static PyBot pyBot;

	public RBMC() {
		board = getBoard();
		for (int i = 0; i < botCount; i++)
			bots.add(new TrainingBot(readGenes));
		if (!human)
			run();
		else
			play();
	}

	public static void run() {
		for (int gen = 0; gen < generations; gen++) {
			System.out.println("\n\n\n\n==============\nGENERATION " + gen);
			for (int i = 0; i < bots.size(); i+=2) {
					int i2 = i + 1;
					BotGame game = new BotGame(bots.get(i), bots.get(i2), i == 0 && i2 == 1, gen);
					System.out.println("Generation " + gen + ", Bots " + i + " vs " + (i2));
//					try {
//						int winner = game.run();
//						if(winner != 0){
//							if(winner == 1)
//								total++;
//							count++;
//						}
//						System.out.println("Winner: " + winner + ", White win percent: " + ((double) (total) / (count + 1)) + ", White wins: " + total);
//						if ((winner & 1) == 1)
//							bots.get(winner == 1 ? i : i2).score--;
//						else if (winner == 0) {
//							bots.get(i2).score -= 2;
//							bots.get(i).score -= 2;
//						}
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
					try {
						int winner = game.run();

						if (winner != 0) {
							if (winner == 1)
								total++;
							count++;
						}
						//System.out.println("Winner: " + winner + ", White win percent: " + ((double) (total) / (count + 1)) + ", White wins: " + total);
						if ((winner & 1) == 1){
							bots.get(winner == 1 ? i2 : i).score--;
							bots.get(winner == 1 ? i : i2).score++;
						}
						else if (winner == 0) {
							bots.get(i2).score -= 2;
							bots.get(i).score -= 2;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
			}
			for (int i = bots.size() - 1; i >= 0; i--){
				if (bots.get(i).score < 0)
					bots.remove(i);
				else
					bots.get(i).score = 0;
			}
			System.out.println("\n\n\n\n\n\n\n" + bots.size());
			while (bots.size() < botCount)
				bots.add(new TrainingBot());
			writeGenes();
		}
	}

	public static void play() {
		pyBot = new PyBot(readGenes);
		while (true)
			pyBot.tryMove();
	}

	public static void writeGenes() {
		File f = new File("Genes.txt");
		String data = "";
		for (int i = 0; i < bots.get(0).genes.length; i++)
			data += bots.get(0).genes[i] + System.lineSeparator();
		for (int i = 0; i < bots.get(0).convolutions.length; i++)
			for (int j = 0; j < bots.get(0).convolutions[i].length; j++)
				data += bots.get(0).convolutions[i][j] + System.lineSeparator();

		try {
			FileWriter fw = new FileWriter(f.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(data);
			bw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public static char[][] getBoard() {
		char[][] ret = new char[8][8];
		ret[0] = new char[] { 'r', 'n', 'b', 'q', 'k', 'b', 'n', 'r' };
		ret[1] = new char[] { 'p', 'p', 'p', 'p', 'p', 'p', 'p', 'p' };
		for (int i = 2; i < 6; i++) {
			for (int j = 0; j < 8; j++) {
				ret[i][j] = '.';
			}
		}
		ret[6] = new char[] { 'P', 'P', 'P', 'P', 'P', 'P', 'P', 'P' };
		ret[7] = new char[] { 'R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R' };
		return ret;
	}

	public static void main(String[] args) {
		RBMC go = new RBMC();
	}
}
