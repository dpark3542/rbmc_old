package game;

import bots.Bot;
import bots.NeuralBot;
import bots.TrainingBot;

/**
 * Created by parkd1 on 7/13/2017.
 */
public class BotGame {
	private Bot a, b;
	public int gen;
	private boolean verbose;

	public BotGame(Bot a, Bot b, boolean verbose, int genr) {
		this.a = a;
		this.b = b;
		gen = genr;
		this.verbose = verbose;
	}

	private static int color(char piece) {
		if (piece == '.') {
			return 0;
		}
		return 'A' <= piece && piece <= 'Z' ? 1 : -1;
	}

	public int run() throws Exception {
		int mc = 0;
		char[][] board = new char[8][8];
		board[0] = new char[] {'r', 'n', 'b', 'q', 'k', 'b', 'n', 'r'};
		board[1] = new char[] {'p', 'p', 'p', 'p', 'p', 'p', 'p', 'p'};
		for (int i = 2; i < 6; i++)
			for (int j = 0; j < 8; j++)
				board[i][j] = '.';
		board[6] = new char[] {'P', 'P', 'P', 'P', 'P', 'P', 'P', 'P'};
		board[7] = new char[] {'R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R'};
		boolean[] castle = {true, true, true, true};
		int[] capture = {-1, -1};
		int[] ep = {-1, -1};
		a.gameStart(1, this);
		b.gameStart(-1, this);
		while (true) {
			if(mc++ > 40 + 50 / (gen + 1))
				return 0;
			// white
			// turn start
			a.turnStart(capture);
			// sense phase
			int[] sense = a.sensePhase();
			// sense results
			int len = 9;
			if (sense[0] == 0 || sense[0] == 7) {
				len = 6;
				if (sense[1] == 0 || sense[1] == 7) {
					len = 4;
				}
			}
			if(sense[0] == -1)
				len = 64;
			int cur = 0;
			int[][] sr = new int[len][3];
			if(sense[0] == -1){
				for (int y = 0; y < 8; y++) {
					for (int x = 0; x < 8; x++) {
						sr[cur] = new int[] {y, x, (int) board[y][x]};
						cur++;
					}
				}
			}else{
				for (int y = sense[0] - 1; y <= sense[0] + 1; y++) {
					for (int x = sense[1] - 1; x <= sense[1] + 1; x++) {
						if (0 <= x && x < 8 && 0 <= y && y < 8) {
							sr[cur] = new int[] {y, x, (int) board[y][x]};
							cur++;
						}
					}
				}
			}
			a.senseResults(sr);
			// move phase
			int[] move = a.movePhase();
			int yi = move[0], xi = move[1], yf = move[2], xf = move[3];
			int[] end = new int[2];
			boolean needPromote = false;
			char p = board[yi][xi];
			if (p == '.' || color(p) != 1) {
				throw new Exception("invalid move");
			}
			if (yi == yf && xi == xf) {
				throw new Exception("invalid move");
			}
			switch (p) {
				case 'P':
					if (yi - yf == 2) {
						ep = new int[] {-1, -1};
						if (xf - xi != 0 || yi != 6 || color(board[yi - 1][xf]) == 1 || color(board[yf][xf]) == 1) {
							throw new Exception("invalid move");
						}
						capture = new int[] {-1, -1};
						if (color(board[yi - 1][xf]) == -1) {
							end = new int[] {yi, xi};
						}
						else if (color(board[yf][xf]) == -1) {
							end = new int[] {yi - 1, xi};
						}
						else {
							end = new int[] {yf, xf};
							ep = new int[] {(yf + yi) / 2, xf};
						}
						board[yi][xi] = '.';
						board[end[0]][end[1]] = 'P';
					}
					else if (yi - yf == 1) {
						if (xf - xi == 0) {
							if (color(board[yi - 1][xf]) == 1) {
								throw new Exception("invalid move");
							}
							capture = new int[] {-1, -1};
							if (color(board[yi - 1][xf]) == -1) {
								end = new int[] {yi, xi};
							}
							else {
								end = new int[] {yf, xf};
							}
							board[yi][xi] = '.';
							board[end[0]][end[1]] = 'P';
						}
						else if (Math.abs(xf - xi) == 1) {
							if (color(board[yf][xf]) == 1) {
								throw new Exception("invalid move");
							}
							else if (yf == ep[0] && xf == ep[1]) {
								board[yi][xf] = '.';
								capture = new int[] {yi, xf};
								end = new int[] {yi, xi};
							}
							else if (board[yf][xf] == '.') {
								capture = new int[] {-1, -1};
								end = new int[] {yi, xi};
							}
							else {
								capture = new int[] {yf, xf};
								end = new int[] {yf, xf};
							}
							board[yi][xi] = '.';
							board[end[0]][end[1]] = 'P';
						}
						else {
							throw new Exception("invalid move");
						}
						ep = new int[] {-1, -1};
					}
					else {
						throw new Exception("invalid move");
					}
					if (end[0] == 0) {
						needPromote = true;
					}
					break;
				case 'R':
				case 'Q':
				case 'B':
					int dy = (int) Math.signum(yf - yi), dx = (int) Math.signum(xf - xi);
					if (dy == 0 && dx == 0) {
						throw new Exception("invalid move");
					}
					if (p == 'B' && Math.abs(dy) != Math.abs(dx)) {
						throw new Exception("invalid move");
					}
					if (p == 'R' && dy != 0 && dx != 0) {
						throw new Exception("invalid move");
					}
					if (p == 'Q' && dy != 0 && dx != 0 && Math.abs(dy) != Math.abs(dx)) {
						throw new Exception("invalid move");
					}
					for (int i = 0; i < Math.max(Math.abs(yf - yi), Math.abs(xf - xi)); i++) {
						int ny = yi + (i + 1) * dy, nx = xi + (i + 1) * dx;
						char q = board[ny][nx];
						if (color(q) == 1) {
							throw new Exception("invalid move");
						}
						else if (color(q) == -1) {
							capture = new int[] {ny, nx};
							end = new int[] {ny, nx};
							break;
						}
						else if (ny == yf && nx == xf) {
							capture = new int[] {-1, -1};
							end = new int[] {yf, xf};
						}
					}
					board[end[0]][end[1]] = board[yi][xi];
					board[yi][xi] = '.';
					break;
				case 'K':
					if (Math.abs(xf - xi) <= 1) {
						if (Math.abs(yf - yi) > 1) {
							throw new Exception("invalid move");
						}
						char q = board[yf][xf];
						if (color(q) == 1) {
							throw new Exception("invalid move");
						}
						else if (color(q) == -1) {
							capture = new int[] {yf, xf};
							end = new int[] {yf, xf};
						}
						else {
							capture = new int[] {-1, -1};
							end = new int[] {yf, xf};
						}
						board[yf][xf] = board[yi][xi];
						board[yi][xi] = '.';
					}
					else if (Math.abs(xf - xi) == 2) {
						if (xf - xi == 2) {
							if (castle[0] && board[7][5] == '.' && board[7][6] == '.') {
								capture = new int[] {-1, -1};
								end = new int[] {yf, xf};
								board[7][4] = '.';
								board[7][5] = 'R';
								board[7][6] = 'K';
								board[7][7] = '.';
							}
							else {
								throw new Exception("invalid move");
							}
						}
						else {
							if (castle[1] && board[7][3] == '.' && board[7][2] == '.' && board[7][1] == '.') {
								capture = new int[] {-1, -1};
								end = new int[] {yf, xf};
								board[7][4] = '.';
								board[7][3] = 'R';
								board[7][2] = 'K';
								board[7][1] = '.';
								board[7][0] = '.';
							}
							else {
								throw new Exception("invalid move");
							}
						}
					}
					else {
						throw new Exception("invalid move");
					}
					break;
				case 'N':
					if (!(Math.abs(xf - xi) == 1 && Math.abs(yf - yi) == 2 || Math.abs(xf - xi) == 2 && Math.abs(yf - yi) == 1)) {
						throw new Exception("invalid move");
					}
					char q = board[yf][xf];
					if (color(q) == 1) {
						throw new Exception("invalid move");
					}
					else if (color(q) == -1) {
						capture = new int[] {yf, xf};
						end = new int[] {yf, xf};
					}
					else {
						capture = new int[] {-1, -1};
						end = new int[] {yf, xf};
					}
					board[yi][xi] = '.';
					board[end[0]][end[1]] = 'N';
					break;
			}
			if (p != 'P') {
				ep = new int[] {-1, -1};
			}
			boolean flag = true;
			for (int i = 0; i < 8; i++) {
				for (int j = 0; j < 8; j++) {
					if (board[i][j] == 'k') {
						flag = false;
					}
				}
			}
			if (flag) {
				return 1;
			}
			// move results
			a.moveResults(new int[] {end[0], end[1], capture[0], capture[1]});
			// promotion
			if (needPromote) {
				char c = a.promotePhase();
				for (int i = 0; i < 8; i++) {
					if (board[0][i] == 'P') {
						board[0][i] = c;
						break;
					}
				}
				a.promoteResults();
			}
			if(verbose)
				printBoard(board);
			// black
//			System.out.println("Black's turn: ");
			// turn start
			b.turnStart(capture);
//			System.out.println("turnStart: " + b.boardStates.size());
//			System.out.println(b.checkBoardStates(board));
			// sense phase
			sense = b.sensePhase();
			// sense results
			len = 9;
			if (sense[0] == 0 || sense[0] == 7) {
				len = 6;
				if (sense[1] == 0 || sense[1] == 7) {
					len = 4;
				}
			}
			if(sense[0] == -1)
				len = 64;
			cur = 0;
			sr = new int[len][3];
			if(sense[0] == -1){
				for (int y = 0; y < 8; y++) {
					for (int x = 0; x < 8; x++) {
						sr[cur] = new int[] {y, x, (int) board[y][x]};
						cur++;
					}
				}
			}else{
				for (int y = sense[0] - 1; y <= sense[0] + 1; y++) {
					for (int x = sense[1] - 1; x <= sense[1] + 1; x++) {
						if (0 <= x && x < 8 && 0 <= y && y < 8) {
							sr[cur] = new int[] {y, x, (int) board[y][x]};
							cur++;
						}
					}
				}
			}

			b.senseResults(sr);
//			System.out.println("senseResults: " + b.boardStates.size());
//			System.out.println(b.checkBoardStates(board));
			// move phase`
			move = b.movePhase();
//			System.out.println("Black move: " + move[0] + " " + move[1] + " " + move[2] + " " + move[3]);
			yi = move[0];
			xi = move[1];
			yf = move[2];
			xf = move[3];
			end = new int[2];
			needPromote = false;
			p = board[yi][xi];
			if (p == '.' || color(p) != -1) {
				throw new Exception("invalid move");
			}
			if (yi == yf && xi == xf) {
				throw new Exception("invalid move");
			}
			switch (p) {
				case 'p':
					if (yf - yi == 2) {
						ep = new int[] {-1, -1};
						if (xf - xi != 0 || yi != 1 || color(board[yi + 1][xf]) == -1 || color(board[yf][xf]) == -1) {
							throw new Exception("invalid move");
						}
						capture = new int[] {-1, -1};
						if (color(board[yi + 1][xf]) == 1) {
							end = new int[] {yi, xi};
						}
						else if (color(board[yf][xf]) == 1) {
							end = new int[] {yi + 1, xi};
						}
						else {
							end = new int[] {yf, xf};
							ep = new int[] {(yf + yi) / 2, xf};
						}
						board[yi][xi] = '.';
						board[end[0]][end[1]] = 'p';
					}
					else if (yf - yi == 1) {
						if (xf - xi == 0) {
							if (color(board[yi + 1][xf]) == -1) {
								throw new Exception("invalid move");
							}
							capture = new int[] {-1, -1};
							if (color(board[yi + 1][xf]) == 1) {
								end = new int[] {yi, xi};
							}
							else {
								end = new int[] {yf, xf};
							}
							board[yi][xi] = '.';
							board[end[0]][end[1]] = 'p';
						}
						else if (Math.abs(xf - xi) == 1) {
							if (color(board[yf][xf]) == -1) {
								throw new Exception("invalid move");
							}
							else if (yf == ep[0] && xf == ep[1]) {
								board[yi][xf] = '.';
								capture = new int[] {yi, xf};
								end = new int[] {yi, xi};
							}
							else if (board[yf][xf] == '.') {
								capture = new int[] {-1, -1};
								end = new int[] {yi, xi};
							}
							else {
								capture = new int[] {yf, xf};
								end = new int[] {yf, xf};
							}
							board[yi][xi] = '.';
							board[end[0]][end[1]] = 'p';
						}
						else {
							throw new Exception("invalid move");
						}
						ep = new int[] {-1, -1};
					}
					else {
						throw new Exception("invalid move");
					}
					if (end[0] == 7) {
						needPromote = true;
					}
					break;
				case 'r':
				case 'q':
				case 'b':
					int dy = (int) Math.signum(yf - yi), dx = (int) Math.signum(xf - xi);
					if (dy == 0 && dx == 0) {
						throw new Exception("invalid move");
					}
					if (p == 'b' && Math.abs(dy) != Math.abs(dx)) {
						throw new Exception("invalid move");
					}
					if (p == 'r' && dy != 0 && dx != 0) {
						throw new Exception("invalid move");
					}
					if (p == 'q' && dy != 0 && dx != 0 && Math.abs(dy) != Math.abs(dx)) {
						throw new Exception("invalid move");
					}
					for (int i = 0; i < Math.max(Math.abs(yf - yi), Math.abs(xf - xi)); i++) {
						int ny = yi + (i + 1) * dy, nx = xi + (i + 1) * dx;
						char q = board[ny][nx];
						if (color(q) == -1) {
							throw new Exception("invalid move");
						}
						else if (color(q) == 1) {
							capture = new int[] {ny, nx};
							end = new int[] {ny, nx};
							break;
						}
						else if (ny == yf && nx == xf) {
							capture = new int[] {-1, -1};
							end = new int[] {yf, xf};
						}
					}
					board[end[0]][end[1]] = board[yi][xi];
					board[yi][xi] = '.';
					break;
				case 'k':
					if (Math.abs(xf - xi) <= 1) {
						if (Math.abs(yf - yi) > 1) {
							throw new Exception("invalid move");
						}
						char q = board[yf][xf];
						if (color(q) == -1) {
							throw new Exception("invalid move");
						}
						else if (color(q) == 1) {
							capture = new int[] {yf, xf};
							end = new int[] {yf, xf};
						}
						else {
							capture = new int[] {-1, -1};
							end = new int[] {yf, xf};
						}
						board[yf][xf] = board[yi][xi];
						board[yi][xi] = '.';
					}
					else if (Math.abs(xf - xi) == 2) {
						if (xf - xi == 2) {
							if (castle[2] && board[0][5] == '.' && board[0][6] == '.') {
								capture = new int[] {-1, -1};
								end = new int[] {yf, xf};
								board[0][4] = '.';
								board[0][5] = 'r';
								board[0][6] = 'k';
								board[0][7] = '.';
							}
							else {
								throw new Exception("invalid move");
							}
						}
						else {
							if (castle[3] && board[0][3] == '.' && board[0][2] == '.' && board[0][1] == '.') {
								capture = new int[] {-1, -1};
								end = new int[] {yf, xf};
								board[0][4] = '.';
								board[0][3] = 'r';
								board[0][2] = 'k';
								board[0][1] = '.';
								board[0][0] = '.';
							}
							else {
								throw new Exception("invalid move");
							}
						}
					}
					else {
						throw new Exception("invalid move");
					}
					break;
				case 'n':
					if (!(Math.abs(xf - xi) == 1 && Math.abs(yf - yi) == 2 || Math.abs(xf - xi) == 2 && Math.abs(yf - yi) == 1)) {
						throw new Exception("invalid move");
					}
					char q = board[yf][xf];
					if (color(q) == -1) {
						throw new Exception("invalid move");
					}
					else if (color(q) == 1) {
						capture = new int[] {yf, xf};
						end = new int[] {yf, xf};
					}
					else {
						capture = new int[] {-1, -1};
						end = new int[] {yf, xf};
					}
					board[yi][xi] = '.';
					board[end[0]][end[1]] = 'n';
					break;
			}
			if (p != 'p') {
				ep = new int[] {-1, -1};
			}
			flag = true;
			for (int i = 0; i < 8; i++) {
				for (int j = 0; j < 8; j++) {
					if (board[i][j] == 'K') {
						flag = false;
					}
				}
			}
			if (flag) {
				return -1;
			}
			// move results
			b.moveResults(new int[] {end[0], end[1], capture[0], capture[1]});
//			System.out.println("moveResults: " + b.boardStates.size());
//			System.out.println(b.checkBoardStates(board));
			// promotion
			if (needPromote) {
				char c = b.promotePhase();
				for (int i = 0; i < 8; i++) {
					if (board[7][i] == 'p') {
						board[7][i] = c;
						break;
					}
				}
				b.promoteResults();
			}
			if(verbose)
				printBoard(board);
//			System.out.println("Black: ");
//			flag = true;
//			for (int i = 0; i < 8; i++) {
//				for (int j = 0; j < 8; j++) {
//					if (board[i][j] != prev[i][j]) {
//						flag = false;
//					}
//					prev[i][j] = board[i][j];
//				}
//			}
//			if (flag) {
//				return 0;
//			}
		}
	}
	
	private static void printBoard(char[][] board) {
		String pieces = "♚♛♜♝♞♟♔♕♖♗♘♙.", chars = "kqrbnpKQRBNP.";
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				System.out.print(pieces.charAt(chars.indexOf(board[j][i])) + " ");
//				System.out.print(board[j][i] + " ");
			}
			System.out.println();
		}
		System.out.println();
	}
}
