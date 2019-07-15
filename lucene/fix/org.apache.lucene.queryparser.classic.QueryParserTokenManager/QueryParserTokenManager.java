

import java.io.IOException;
import org.apache.lucene.queryparser.classic.CharStream;
import org.apache.lucene.queryparser.classic.QueryParserConstants;
import org.apache.lucene.queryparser.classic.Token;


@SuppressWarnings("cast")
public class QueryParserTokenManager implements QueryParserConstants {
	private final int jjStopStringLiteralDfa_2(int pos, long active0) {
		switch (pos) {
			default :
				return -1;
		}
	}

	private final int jjStartNfa_2(int pos, long active0) {
		return jjMoveNfa_2(jjStopStringLiteralDfa_2(pos, active0), (pos + 1));
	}

	private int jjStopAtPos(int pos, int kind) {
		jjmatchedKind = kind;
		jjmatchedPos = pos;
		return pos + 1;
	}

	private int jjMoveStringLiteralDfa0_2() {
		switch (curChar) {
			case 40 :
				return jjStopAtPos(0, 14);
			case 41 :
				return jjStopAtPos(0, 15);
			case 42 :
				return jjStartNfaWithStates_2(0, 17, 49);
			case 43 :
				return jjStartNfaWithStates_2(0, 11, 15);
			case 45 :
				return jjStartNfaWithStates_2(0, 12, 15);
			case 58 :
				return jjStopAtPos(0, 16);
			case 91 :
				return jjStopAtPos(0, 25);
			case 94 :
				return jjStopAtPos(0, 18);
			case 123 :
				return jjStopAtPos(0, 26);
			default :
				return jjMoveNfa_2(0, 0);
		}
	}

	private int jjStartNfaWithStates_2(int pos, int kind, int state) {
		jjmatchedKind = kind;
		jjmatchedPos = pos;
		try {
			curChar = input_stream.readChar();
		} catch (IOException e) {
			return pos + 1;
		}
		return jjMoveNfa_2(state, (pos + 1));
	}

	static final long[] jjbitVec0 = new long[]{ 1L, 0L, 0L, 0L };

	static final long[] jjbitVec1 = new long[]{ -2L, -1L, -1L, -1L };

	static final long[] jjbitVec3 = new long[]{ 0L, 0L, -1L, -1L };

	static final long[] jjbitVec4 = new long[]{ -281474976710658L, -1L, -1L, -1L };

	private int jjMoveNfa_2(int startState, int curPos) {
		int startsAt = 0;
		jjnewStateCnt = 49;
		int i = 1;
		jjstateSet[0] = startState;
		int kind = 2147483647;
		for (; ;) {
			if ((++(jjround)) == 2147483647)
				ReInitRounds();

			if ((curChar) < 64) {
				long l = 1L << (curChar);
				do {
					switch (jjstateSet[(--i)]) {
						case 49 :
						case 33 :
							if ((-288374442239731201L & l) == 0L)
								break;

							if (kind > 23)
								kind = 23;

							jjCheckNAddTwoStates(33, 34);
							break;
						case 0 :
							if ((-288418422704842241L & l) != 0L) {
								if (kind > 23)
									kind = 23;

								jjCheckNAddTwoStates(33, 34);
							}else
								if ((4294977024L & l) != 0L) {
									if (kind > 7)
										kind = 7;

								}else
									if ((43989055045632L & l) != 0L)
										jjstateSet[((jjnewStateCnt)++)] = 15;
									else
										if ((curChar) == 47)
											jjCheckNAddStates(0, 2);
										else
											if ((curChar) == 34)
												jjCheckNAddStates(3, 5);





							if ((8934949216103422463L & l) != 0L) {
								if (kind > 20)
									kind = 20;

								jjCheckNAddStates(6, 10);
							}else
								if ((curChar) == 42) {
									if (kind > 22)
										kind = 22;

								}else
									if ((curChar) == 33) {
										if (kind > 10)
											kind = 10;

									}


							if ((curChar) == 38)
								jjstateSet[((jjnewStateCnt)++)] = 4;

							break;
						case 4 :
							if (((curChar) == 38) && (kind > 8))
								kind = 8;

							break;
						case 5 :
							if ((curChar) == 38)
								jjstateSet[((jjnewStateCnt)++)] = 4;

							break;
						case 13 :
							if (((curChar) == 33) && (kind > 10))
								kind = 10;

							break;
						case 14 :
							if ((43989055045632L & l) != 0L)
								jjstateSet[((jjnewStateCnt)++)] = 15;

							break;
						case 15 :
							if (((4294977024L & l) != 0L) && (kind > 13))
								kind = 13;

							break;
						case 16 :
							if ((curChar) == 34)
								jjCheckNAddStates(3, 5);

							break;
						case 17 :
							if ((-17179869185L & l) != 0L)
								jjCheckNAddStates(3, 5);

							break;
						case 19 :
							jjCheckNAddStates(3, 5);
							break;
						case 20 :
							if (((curChar) == 34) && (kind > 19))
								kind = 19;

							break;
						case 22 :
							if ((287948901175001088L & l) == 0L)
								break;

							if (kind > 21)
								kind = 21;

							jjCheckNAddStates(11, 14);
							break;
						case 23 :
							if ((curChar) == 46)
								jjCheckNAdd(24);

							break;
						case 24 :
							if ((287948901175001088L & l) == 0L)
								break;

							if (kind > 21)
								kind = 21;

							jjCheckNAddStates(15, 17);
							break;
						case 25 :
							if ((8934993196568533503L & l) == 0L)
								break;

							if (kind > 21)
								kind = 21;

							jjCheckNAddTwoStates(25, 26);
							break;
						case 27 :
							if (kind > 21)
								kind = 21;

							jjCheckNAddTwoStates(25, 26);
							break;
						case 28 :
							if ((8934993196568533503L & l) == 0L)
								break;

							if (kind > 21)
								kind = 21;

							jjCheckNAddTwoStates(28, 29);
							break;
						case 30 :
							if (kind > 21)
								kind = 21;

							jjCheckNAddTwoStates(28, 29);
							break;
						case 31 :
							if (((curChar) == 42) && (kind > 22))
								kind = 22;

							break;
						case 32 :
							if ((-288418422704842241L & l) == 0L)
								break;

							if (kind > 23)
								kind = 23;

							jjCheckNAddTwoStates(33, 34);
							break;
						case 35 :
							if (kind > 23)
								kind = 23;

							jjCheckNAddTwoStates(33, 34);
							break;
						case 36 :
						case 38 :
							if ((curChar) == 47)
								jjCheckNAddStates(0, 2);

							break;
						case 37 :
							if ((-140737488355329L & l) != 0L)
								jjCheckNAddStates(0, 2);

							break;
						case 40 :
							if (((curChar) == 47) && (kind > 24))
								kind = 24;

							break;
						case 41 :
							if ((8934949216103422463L & l) == 0L)
								break;

							if (kind > 20)
								kind = 20;

							jjCheckNAddStates(6, 10);
							break;
						case 42 :
							if ((8934993196568533503L & l) == 0L)
								break;

							if (kind > 20)
								kind = 20;

							jjCheckNAddTwoStates(42, 43);
							break;
						case 44 :
							if (kind > 20)
								kind = 20;

							jjCheckNAddTwoStates(42, 43);
							break;
						case 45 :
							if ((8934993196568533503L & l) != 0L)
								jjCheckNAddStates(18, 20);

							break;
						case 47 :
							jjCheckNAddStates(18, 20);
							break;
						default :
							break;
					}
				} while (i != startsAt );
			}else
				if ((curChar) < 128) {
					long l = 1L << ((curChar) & 63);
					do {
						switch (jjstateSet[(--i)]) {
							case 49 :
								if ((-7493989781957771265L & l) != 0L) {
									if (kind > 23)
										kind = 23;

									jjCheckNAddTwoStates(33, 34);
								}else
									if ((curChar) == 92)
										jjCheckNAdd(35);


								break;
							case 0 :
								if ((-7493989781957771265L & l) != 0L) {
									if (kind > 20)
										kind = 20;

									jjCheckNAddStates(6, 10);
								}else
									if ((curChar) == 92)
										jjCheckNAddStates(21, 23);
									else
										if ((curChar) == 126) {
											if (kind > 21)
												kind = 21;

											jjCheckNAddStates(24, 26);
										}


								if ((-7493989781957771265L & l) != 0L) {
									if (kind > 23)
										kind = 23;

									jjCheckNAddTwoStates(33, 34);
								}
								if ((curChar) == 78)
									jjstateSet[((jjnewStateCnt)++)] = 11;
								else
									if ((curChar) == 124)
										jjstateSet[((jjnewStateCnt)++)] = 8;
									else
										if ((curChar) == 79)
											jjstateSet[((jjnewStateCnt)++)] = 6;
										else
											if ((curChar) == 65)
												jjstateSet[((jjnewStateCnt)++)] = 2;




								break;
							case 1 :
								if (((curChar) == 68) && (kind > 8))
									kind = 8;

								break;
							case 2 :
								if ((curChar) == 78)
									jjstateSet[((jjnewStateCnt)++)] = 1;

								break;
							case 3 :
								if ((curChar) == 65)
									jjstateSet[((jjnewStateCnt)++)] = 2;

								break;
							case 6 :
								if (((curChar) == 82) && (kind > 9))
									kind = 9;

								break;
							case 7 :
								if ((curChar) == 79)
									jjstateSet[((jjnewStateCnt)++)] = 6;

								break;
							case 8 :
								if (((curChar) == 124) && (kind > 9))
									kind = 9;

								break;
							case 9 :
								if ((curChar) == 124)
									jjstateSet[((jjnewStateCnt)++)] = 8;

								break;
							case 10 :
								if (((curChar) == 84) && (kind > 10))
									kind = 10;

								break;
							case 11 :
								if ((curChar) == 79)
									jjstateSet[((jjnewStateCnt)++)] = 10;

								break;
							case 12 :
								if ((curChar) == 78)
									jjstateSet[((jjnewStateCnt)++)] = 11;

								break;
							case 17 :
								if ((-268435457L & l) != 0L)
									jjCheckNAddStates(3, 5);

								break;
							case 18 :
								if ((curChar) == 92)
									jjstateSet[((jjnewStateCnt)++)] = 19;

								break;
							case 19 :
								jjCheckNAddStates(3, 5);
								break;
							case 21 :
								if ((curChar) != 126)
									break;

								if (kind > 21)
									kind = 21;

								jjCheckNAddStates(24, 26);
								break;
							case 25 :
								if ((-7493989781957771265L & l) == 0L)
									break;

								if (kind > 21)
									kind = 21;

								jjCheckNAddTwoStates(25, 26);
								break;
							case 26 :
								if ((curChar) == 92)
									jjstateSet[((jjnewStateCnt)++)] = 27;

								break;
							case 27 :
								if (kind > 21)
									kind = 21;

								jjCheckNAddTwoStates(25, 26);
								break;
							case 28 :
								if ((-7493989781957771265L & l) == 0L)
									break;

								if (kind > 21)
									kind = 21;

								jjCheckNAddTwoStates(28, 29);
								break;
							case 29 :
								if ((curChar) == 92)
									jjstateSet[((jjnewStateCnt)++)] = 30;

								break;
							case 30 :
								if (kind > 21)
									kind = 21;

								jjCheckNAddTwoStates(28, 29);
								break;
							case 32 :
								if ((-7493989781957771265L & l) == 0L)
									break;

								if (kind > 23)
									kind = 23;

								jjCheckNAddTwoStates(33, 34);
								break;
							case 33 :
								if ((-7493989781957771265L & l) == 0L)
									break;

								if (kind > 23)
									kind = 23;

								jjCheckNAddTwoStates(33, 34);
								break;
							case 34 :
								if ((curChar) == 92)
									jjCheckNAdd(35);

								break;
							case 35 :
								if (kind > 23)
									kind = 23;

								jjCheckNAddTwoStates(33, 34);
								break;
							case 37 :
								jjAddStates(0, 2);
								break;
							case 39 :
								if ((curChar) == 92)
									jjstateSet[((jjnewStateCnt)++)] = 38;

								break;
							case 41 :
								if ((-7493989781957771265L & l) == 0L)
									break;

								if (kind > 20)
									kind = 20;

								jjCheckNAddStates(6, 10);
								break;
							case 42 :
								if ((-7493989781957771265L & l) == 0L)
									break;

								if (kind > 20)
									kind = 20;

								jjCheckNAddTwoStates(42, 43);
								break;
							case 43 :
								if ((curChar) == 92)
									jjCheckNAdd(44);

								break;
							case 44 :
								if (kind > 20)
									kind = 20;

								jjCheckNAddTwoStates(42, 43);
								break;
							case 45 :
								if ((-7493989781957771265L & l) != 0L)
									jjCheckNAddStates(18, 20);

								break;
							case 46 :
								if ((curChar) == 92)
									jjCheckNAdd(47);

								break;
							case 47 :
								jjCheckNAddStates(18, 20);
								break;
							case 48 :
								if ((curChar) == 92)
									jjCheckNAddStates(21, 23);

								break;
							default :
								break;
						}
					} while (i != startsAt );
				}else {
					int hiByte = ((int) ((curChar) >> 8));
					int i1 = hiByte >> 6;
					long l1 = 1L << (hiByte & 63);
					int i2 = ((curChar) & 255) >> 6;
					long l2 = 1L << ((curChar) & 63);
					do {
						switch (jjstateSet[(--i)]) {
							case 49 :
							case 33 :
								if (!(QueryParserTokenManager.jjCanMove_2(hiByte, i1, i2, l1, l2)))
									break;

								if (kind > 23)
									kind = 23;

								jjCheckNAddTwoStates(33, 34);
								break;
							case 0 :
								if (QueryParserTokenManager.jjCanMove_0(hiByte, i1, i2, l1, l2)) {
									if (kind > 7)
										kind = 7;

								}
								if (QueryParserTokenManager.jjCanMove_2(hiByte, i1, i2, l1, l2)) {
									if (kind > 23)
										kind = 23;

									jjCheckNAddTwoStates(33, 34);
								}
								if (QueryParserTokenManager.jjCanMove_2(hiByte, i1, i2, l1, l2)) {
									if (kind > 20)
										kind = 20;

									jjCheckNAddStates(6, 10);
								}
								break;
							case 15 :
								if ((QueryParserTokenManager.jjCanMove_0(hiByte, i1, i2, l1, l2)) && (kind > 13))
									kind = 13;

								break;
							case 17 :
							case 19 :
								if (QueryParserTokenManager.jjCanMove_1(hiByte, i1, i2, l1, l2))
									jjCheckNAddStates(3, 5);

								break;
							case 25 :
								if (!(QueryParserTokenManager.jjCanMove_2(hiByte, i1, i2, l1, l2)))
									break;

								if (kind > 21)
									kind = 21;

								jjCheckNAddTwoStates(25, 26);
								break;
							case 27 :
								if (!(QueryParserTokenManager.jjCanMove_1(hiByte, i1, i2, l1, l2)))
									break;

								if (kind > 21)
									kind = 21;

								jjCheckNAddTwoStates(25, 26);
								break;
							case 28 :
								if (!(QueryParserTokenManager.jjCanMove_2(hiByte, i1, i2, l1, l2)))
									break;

								if (kind > 21)
									kind = 21;

								jjCheckNAddTwoStates(28, 29);
								break;
							case 30 :
								if (!(QueryParserTokenManager.jjCanMove_1(hiByte, i1, i2, l1, l2)))
									break;

								if (kind > 21)
									kind = 21;

								jjCheckNAddTwoStates(28, 29);
								break;
							case 32 :
								if (!(QueryParserTokenManager.jjCanMove_2(hiByte, i1, i2, l1, l2)))
									break;

								if (kind > 23)
									kind = 23;

								jjCheckNAddTwoStates(33, 34);
								break;
							case 35 :
								if (!(QueryParserTokenManager.jjCanMove_1(hiByte, i1, i2, l1, l2)))
									break;

								if (kind > 23)
									kind = 23;

								jjCheckNAddTwoStates(33, 34);
								break;
							case 37 :
								if (QueryParserTokenManager.jjCanMove_1(hiByte, i1, i2, l1, l2))
									jjAddStates(0, 2);

								break;
							case 41 :
								if (!(QueryParserTokenManager.jjCanMove_2(hiByte, i1, i2, l1, l2)))
									break;

								if (kind > 20)
									kind = 20;

								jjCheckNAddStates(6, 10);
								break;
							case 42 :
								if (!(QueryParserTokenManager.jjCanMove_2(hiByte, i1, i2, l1, l2)))
									break;

								if (kind > 20)
									kind = 20;

								jjCheckNAddTwoStates(42, 43);
								break;
							case 44 :
								if (!(QueryParserTokenManager.jjCanMove_1(hiByte, i1, i2, l1, l2)))
									break;

								if (kind > 20)
									kind = 20;

								jjCheckNAddTwoStates(42, 43);
								break;
							case 45 :
								if (QueryParserTokenManager.jjCanMove_2(hiByte, i1, i2, l1, l2))
									jjCheckNAddStates(18, 20);

								break;
							case 47 :
								if (QueryParserTokenManager.jjCanMove_1(hiByte, i1, i2, l1, l2))
									jjCheckNAddStates(18, 20);

								break;
							default :
								break;
						}
					} while (i != startsAt );
				}

			if (kind != 2147483647) {
				jjmatchedKind = kind;
				jjmatchedPos = curPos;
				kind = 2147483647;
			}
			++curPos;
			if ((i = jjnewStateCnt) == (startsAt = 49 - (jjnewStateCnt = startsAt)))
				return curPos;

			try {
				curChar = input_stream.readChar();
			} catch (IOException e) {
				return curPos;
			}
		}
	}

	private int jjMoveStringLiteralDfa0_0() {
		return jjMoveNfa_0(0, 0);
	}

	private int jjMoveNfa_0(int startState, int curPos) {
		int startsAt = 0;
		jjnewStateCnt = 3;
		int i = 1;
		jjstateSet[0] = startState;
		int kind = 2147483647;
		for (; ;) {
			if ((++(jjround)) == 2147483647)
				ReInitRounds();

			if ((curChar) < 64) {
				long l = 1L << (curChar);
				do {
					switch (jjstateSet[(--i)]) {
						case 0 :
							if ((287948901175001088L & l) == 0L)
								break;

							if (kind > 27)
								kind = 27;

							jjAddStates(27, 28);
							break;
						case 1 :
							if ((curChar) == 46)
								jjCheckNAdd(2);

							break;
						case 2 :
							if ((287948901175001088L & l) == 0L)
								break;

							if (kind > 27)
								kind = 27;

							jjCheckNAdd(2);
							break;
						default :
							break;
					}
				} while (i != startsAt );
			}else
				if ((curChar) < 128) {
					long l = 1L << ((curChar) & 63);
					do {
						switch (jjstateSet[(--i)]) {
							default :
								break;
						}
					} while (i != startsAt );
				}else {
					int hiByte = ((int) ((curChar) >> 8));
					int i1 = hiByte >> 6;
					long l1 = 1L << (hiByte & 63);
					int i2 = ((curChar) & 255) >> 6;
					long l2 = 1L << ((curChar) & 63);
					do {
						switch (jjstateSet[(--i)]) {
							default :
								break;
						}
					} while (i != startsAt );
				}

			if (kind != 2147483647) {
				jjmatchedKind = kind;
				jjmatchedPos = curPos;
				kind = 2147483647;
			}
			++curPos;
			if ((i = jjnewStateCnt) == (startsAt = 3 - (jjnewStateCnt = startsAt)))
				return curPos;

			try {
				curChar = input_stream.readChar();
			} catch (IOException e) {
				return curPos;
			}
		}
	}

	private final int jjStopStringLiteralDfa_1(int pos, long active0) {
		switch (pos) {
			case 0 :
				if ((active0 & 268435456L) != 0L) {
					jjmatchedKind = 32;
					return 6;
				}
				return -1;
			default :
				return -1;
		}
	}

	private final int jjStartNfa_1(int pos, long active0) {
		return jjMoveNfa_1(jjStopStringLiteralDfa_1(pos, active0), (pos + 1));
	}

	private int jjMoveStringLiteralDfa0_1() {
		switch (curChar) {
			case 84 :
				return jjMoveStringLiteralDfa1_1(268435456L);
			case 93 :
				return jjStopAtPos(0, 29);
			case 125 :
				return jjStopAtPos(0, 30);
			default :
				return jjMoveNfa_1(0, 0);
		}
	}

	private int jjMoveStringLiteralDfa1_1(long active0) {
		try {
			curChar = input_stream.readChar();
		} catch (IOException e) {
			jjStopStringLiteralDfa_1(0, active0);
			return 1;
		}
		switch (curChar) {
			case 79 :
				if ((active0 & 268435456L) != 0L)
					return jjStartNfaWithStates_1(1, 28, 6);

				break;
			default :
				break;
		}
		return jjStartNfa_1(0, active0);
	}

	private int jjStartNfaWithStates_1(int pos, int kind, int state) {
		jjmatchedKind = kind;
		jjmatchedPos = pos;
		try {
			curChar = input_stream.readChar();
		} catch (IOException e) {
			return pos + 1;
		}
		return jjMoveNfa_1(state, (pos + 1));
	}

	private int jjMoveNfa_1(int startState, int curPos) {
		int startsAt = 0;
		jjnewStateCnt = 7;
		int i = 1;
		jjstateSet[0] = startState;
		int kind = 2147483647;
		for (; ;) {
			if ((++(jjround)) == 2147483647)
				ReInitRounds();

			if ((curChar) < 64) {
				long l = 1L << (curChar);
				do {
					switch (jjstateSet[(--i)]) {
						case 0 :
							if ((-4294967297L & l) != 0L) {
								if (kind > 32)
									kind = 32;

								jjCheckNAdd(6);
							}
							if ((4294977024L & l) != 0L) {
								if (kind > 7)
									kind = 7;

							}else
								if ((curChar) == 34)
									jjCheckNAddTwoStates(2, 4);


							break;
						case 1 :
							if ((curChar) == 34)
								jjCheckNAddTwoStates(2, 4);

							break;
						case 2 :
							if ((-17179869185L & l) != 0L)
								jjCheckNAddStates(29, 31);

							break;
						case 3 :
							if ((curChar) == 34)
								jjCheckNAddStates(29, 31);

							break;
						case 5 :
							if (((curChar) == 34) && (kind > 31))
								kind = 31;

							break;
						case 6 :
							if ((-4294967297L & l) == 0L)
								break;

							if (kind > 32)
								kind = 32;

							jjCheckNAdd(6);
							break;
						default :
							break;
					}
				} while (i != startsAt );
			}else
				if ((curChar) < 128) {
					long l = 1L << ((curChar) & 63);
					do {
						switch (jjstateSet[(--i)]) {
							case 0 :
							case 6 :
								if ((-2305843009750564865L & l) == 0L)
									break;

								if (kind > 32)
									kind = 32;

								jjCheckNAdd(6);
								break;
							case 2 :
								jjAddStates(29, 31);
								break;
							case 4 :
								if ((curChar) == 92)
									jjstateSet[((jjnewStateCnt)++)] = 3;

								break;
							default :
								break;
						}
					} while (i != startsAt );
				}else {
					int hiByte = ((int) ((curChar) >> 8));
					int i1 = hiByte >> 6;
					long l1 = 1L << (hiByte & 63);
					int i2 = ((curChar) & 255) >> 6;
					long l2 = 1L << ((curChar) & 63);
					do {
						switch (jjstateSet[(--i)]) {
							case 0 :
								if (QueryParserTokenManager.jjCanMove_0(hiByte, i1, i2, l1, l2)) {
									if (kind > 7)
										kind = 7;

								}
								if (QueryParserTokenManager.jjCanMove_1(hiByte, i1, i2, l1, l2)) {
									if (kind > 32)
										kind = 32;

									jjCheckNAdd(6);
								}
								break;
							case 2 :
								if (QueryParserTokenManager.jjCanMove_1(hiByte, i1, i2, l1, l2))
									jjAddStates(29, 31);

								break;
							case 6 :
								if (!(QueryParserTokenManager.jjCanMove_1(hiByte, i1, i2, l1, l2)))
									break;

								if (kind > 32)
									kind = 32;

								jjCheckNAdd(6);
								break;
							default :
								break;
						}
					} while (i != startsAt );
				}

			if (kind != 2147483647) {
				jjmatchedKind = kind;
				jjmatchedPos = curPos;
				kind = 2147483647;
			}
			++curPos;
			if ((i = jjnewStateCnt) == (startsAt = 7 - (jjnewStateCnt = startsAt)))
				return curPos;

			try {
				curChar = input_stream.readChar();
			} catch (IOException e) {
				return curPos;
			}
		}
	}

	static final int[] jjnextStates = new int[]{ 37, 39, 40, 17, 18, 20, 42, 43, 45, 46, 31, 22, 23, 25, 26, 24, 25, 26, 45, 46, 31, 44, 47, 35, 22, 28, 29, 0, 1, 2, 4, 5 };

	private static final boolean jjCanMove_0(int hiByte, int i1, int i2, long l1, long l2) {
		switch (hiByte) {
			case 48 :
				return ((QueryParserTokenManager.jjbitVec0[i2]) & l2) != 0L;
			default :
				return false;
		}
	}

	private static final boolean jjCanMove_1(int hiByte, int i1, int i2, long l1, long l2) {
		switch (hiByte) {
			case 0 :
				return ((QueryParserTokenManager.jjbitVec3[i2]) & l2) != 0L;
			default :
				if (((QueryParserTokenManager.jjbitVec1[i1]) & l1) != 0L)
					return true;

				return false;
		}
	}

	private static final boolean jjCanMove_2(int hiByte, int i1, int i2, long l1, long l2) {
		switch (hiByte) {
			case 0 :
				return ((QueryParserTokenManager.jjbitVec3[i2]) & l2) != 0L;
			case 48 :
				return ((QueryParserTokenManager.jjbitVec1[i2]) & l2) != 0L;
			default :
				if (((QueryParserTokenManager.jjbitVec4[i1]) & l1) != 0L)
					return true;

				return false;
		}
	}

	public static final String[] jjstrLiteralImages = new String[]{ "", null, null, null, null, null, null, null, null, null, null, "+", "-", null, "(", ")", ":", "*", "^", null, null, null, null, null, null, "[", "{", null, "TO", "]", "}", null, null };

	public static final String[] lexStateNames = new String[]{ "Boost", "Range", "DEFAULT" };

	public static final int[] jjnewLexState = new int[]{ -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, -1, -1, -1, -1, -1, 1, 1, 2, -1, 2, 2, -1, -1 };

	static final long[] jjtoToken = new long[]{ 8589934337L };

	static final long[] jjtoSkip = new long[]{ 128L };

	protected CharStream input_stream;

	private final int[] jjrounds = new int[49];

	private final int[] jjstateSet = new int[98];

	protected char curChar;

	public QueryParserTokenManager(CharStream stream) {
		input_stream = stream;
	}

	public QueryParserTokenManager(CharStream stream, int lexState) {
		this(stream);
		SwitchTo(lexState);
	}

	public void ReInit(CharStream stream) {
		jjmatchedPos = jjnewStateCnt = 0;
		curLexState = defaultLexState;
		input_stream = stream;
		ReInitRounds();
	}

	private void ReInitRounds() {
		int i;
		jjround = -2147483647;
		for (i = 49; (i--) > 0;)
			jjrounds[i] = -2147483648;

	}

	public void ReInit(CharStream stream, int lexState) {
		ReInit(stream);
		SwitchTo(lexState);
	}

	public void SwitchTo(int lexState) {
		if ((lexState >= 3) || (lexState < 0)) {
		}else
			curLexState = lexState;

	}

	protected Token jjFillToken() {
		final Token t;
		final String curTokenImage;
		final int beginLine;
		final int endLine;
		final int beginColumn;
		final int endColumn;
		String im = QueryParserTokenManager.jjstrLiteralImages[jjmatchedKind];
		curTokenImage = (im == null) ? input_stream.GetImage() : im;
		beginLine = input_stream.getBeginLine();
		beginColumn = input_stream.getBeginColumn();
		endLine = input_stream.getEndLine();
		endColumn = input_stream.getEndColumn();
		t = Token.newToken(jjmatchedKind, curTokenImage);
		t.beginLine = beginLine;
		t.endLine = endLine;
		t.beginColumn = beginColumn;
		t.endColumn = endColumn;
		return t;
	}

	int curLexState = 2;

	int defaultLexState = 2;

	int jjnewStateCnt;

	int jjround;

	int jjmatchedPos;

	int jjmatchedKind;

	public Token getNextToken() {
		Token matchedToken;
		int curPos = 0;
		EOFLoop : for (; ;) {
			try {
				curChar = input_stream.BeginToken();
			} catch (IOException e) {
				jjmatchedKind = 0;
				matchedToken = jjFillToken();
				return matchedToken;
			}
			switch (curLexState) {
				case 0 :
					jjmatchedKind = 2147483647;
					jjmatchedPos = 0;
					curPos = jjMoveStringLiteralDfa0_0();
					break;
				case 1 :
					jjmatchedKind = 2147483647;
					jjmatchedPos = 0;
					curPos = jjMoveStringLiteralDfa0_1();
					break;
				case 2 :
					jjmatchedKind = 2147483647;
					jjmatchedPos = 0;
					curPos = jjMoveStringLiteralDfa0_2();
					break;
			}
			if ((jjmatchedKind) != 2147483647) {
				if (((jjmatchedPos) + 1) < curPos)
					input_stream.backup(((curPos - (jjmatchedPos)) - 1));

				if (((QueryParserTokenManager.jjtoToken[((jjmatchedKind) >> 6)]) & (1L << ((jjmatchedKind) & 63))) != 0L) {
					matchedToken = jjFillToken();
					if ((QueryParserTokenManager.jjnewLexState[jjmatchedKind]) != (-1))
						curLexState = QueryParserTokenManager.jjnewLexState[jjmatchedKind];

					return matchedToken;
				}else {
					if ((QueryParserTokenManager.jjnewLexState[jjmatchedKind]) != (-1))
						curLexState = QueryParserTokenManager.jjnewLexState[jjmatchedKind];

					continue EOFLoop;
				}
			}
			int error_line = input_stream.getEndLine();
			int error_column = input_stream.getEndColumn();
			String error_after = null;
			boolean EOFSeen = false;
			try {
				input_stream.readChar();
				input_stream.backup(1);
			} catch (IOException e1) {
				EOFSeen = true;
				error_after = (curPos <= 1) ? "" : input_stream.GetImage();
				if (((curChar) == '\n') || ((curChar) == '\r')) {
					error_line++;
					error_column = 0;
				}else
					error_column++;

			}
			if (!EOFSeen) {
				input_stream.backup(1);
				error_after = (curPos <= 1) ? "" : input_stream.GetImage();
			}
		}
	}

	private void jjCheckNAdd(int state) {
		if ((jjrounds[state]) != (jjround)) {
			jjstateSet[((jjnewStateCnt)++)] = state;
			jjrounds[state] = jjround;
		}
	}

	private void jjAddStates(int start, int end) {
		do {
			jjstateSet[((jjnewStateCnt)++)] = QueryParserTokenManager.jjnextStates[start];
		} while ((start++) != end );
	}

	private void jjCheckNAddTwoStates(int state1, int state2) {
		jjCheckNAdd(state1);
		jjCheckNAdd(state2);
	}

	private void jjCheckNAddStates(int start, int end) {
		do {
			jjCheckNAdd(QueryParserTokenManager.jjnextStates[start]);
		} while ((start++) != end );
	}
}

