package rbmc;

import bots.NeuralBot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class PyBot {
	public char[][] board = new char[8][8], mindBoard = new char[8][8];
	public double[] genes = new double[7171];
	public boolean turnPhase = false;
	public int col, mc = 0;
	public NeuralBot bot;

	public PyBot(boolean read) {
		bot = new NeuralBot(read);
		while (true) {
			tryMove();
		}
	}

	public void tryMove() {
		File f = new File("C:\\Users\\hontzam1\\Desktop\\rbmc\\client\\python");
		Scanner sc;
		try {
			sc = new Scanner(f);
			if (sc.nextLine().equals("TS")) {
				if (sc.hasNextLine()) {
					String captureSquare = sc.nextLine(); // TODO Add to multiHyp
					int cy = 8 - Integer.parseInt("" + captureSquare.charAt(1));
					int cx = captureSquare.charAt(0) - 'a';
//					bot.board[cy][cx] = '.';
				}
				FileWriter fw = new FileWriter(f);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write("R"); // Tell python we got its message
				bw.close();
				fw.close();
			}
			if (sc.nextLine().equals("SP")) {
				FileWriter fw = new FileWriter(f);
				BufferedWriter bw = new BufferedWriter(fw);
				int[] loc = bot.sensePhase();
				bw.write("R:" + loc[0] + ":" + loc[1]); // Tell python we got its message
				bw.close();
				fw.close();
			}
			if (sc.nextLine().equals("SR")) {
				while (sc.hasNextLine()) {
					String str = sc.nextLine(); // TODO Add to multiHyp
					String[] broken = str.split(":");
					int y = 7 - Integer.parseInt(broken[0]); // TODO PyBot still depends on normal bot, which is still integrated with RBMC arbiter. This should be fixed.
					int x = Integer.parseInt(broken[1]);
					char p = broken[2].charAt(0);
//					bot.board[y][x] = p;
				}
				FileWriter fw = new FileWriter(f);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write("R"); // Tell python we got its message
				bw.close();
				fw.close();
			}
			if (sc.nextLine().equals("MP")) {
				FileWriter fw = new FileWriter(f);
				BufferedWriter bw = new BufferedWriter(fw);
				int[] move = bot.movePhase();
				bw.write("R:" + move[0] + ":" + move[1] + ":" + move[2] + ":" + move[3]); // Tell python we got its message
				bw.close();
				fw.close();
			}
			if (sc.nextLine().equals("MR")) {
				if (sc.hasNextLine())
				//Syntax: Next line could say "I" for illegal move
				//Otherwise it says "piece:square:(C if capture, N if not)
				{
					;//Next line is capturePiece, following line is captureSquare. Add to multiHyp
				}
				FileWriter fw = new FileWriter(f);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write("R"); // Tell python we got its message
				bw.close();
				fw.close();
			}
			sc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}