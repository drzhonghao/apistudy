

import org.apache.lucene.util.ArrayUtil;
import org.tartarus.snowball.Among;


public abstract class SnowballProgram {
	protected SnowballProgram() {
		current = new char[8];
		setCurrent("");
	}

	public abstract boolean stem();

	public void setCurrent(String value) {
		current = value.toCharArray();
		cursor = 0;
		limit = value.length();
		limit_backward = 0;
		bra = cursor;
		ket = limit;
	}

	public String getCurrent() {
		return new String(current, 0, limit);
	}

	public void setCurrent(char[] text, int length) {
		current = text;
		cursor = 0;
		limit = length;
		limit_backward = 0;
		bra = cursor;
		ket = limit;
	}

	public char[] getCurrentBuffer() {
		return current;
	}

	public int getCurrentBufferLength() {
		return limit;
	}

	private char[] current;

	protected int cursor;

	protected int limit;

	protected int limit_backward;

	protected int bra;

	protected int ket;

	protected void copy_from(SnowballProgram other) {
		current = other.current;
		cursor = other.cursor;
		limit = other.limit;
		limit_backward = other.limit_backward;
		bra = other.bra;
		ket = other.ket;
	}

	protected boolean in_grouping(char[] s, int min, int max) {
		if ((cursor) >= (limit))
			return false;

		char ch = current[cursor];
		if ((ch > max) || (ch < min))
			return false;

		ch -= min;
		if (((s[(ch >> 3)]) & (1 << (ch & 7))) == 0)
			return false;

		(cursor)++;
		return true;
	}

	protected boolean in_grouping_b(char[] s, int min, int max) {
		if ((cursor) <= (limit_backward))
			return false;

		char ch = current[((cursor) - 1)];
		if ((ch > max) || (ch < min))
			return false;

		ch -= min;
		if (((s[(ch >> 3)]) & (1 << (ch & 7))) == 0)
			return false;

		(cursor)--;
		return true;
	}

	protected boolean out_grouping(char[] s, int min, int max) {
		if ((cursor) >= (limit))
			return false;

		char ch = current[cursor];
		if ((ch > max) || (ch < min)) {
			(cursor)++;
			return true;
		}
		ch -= min;
		if (((s[(ch >> 3)]) & (1 << (ch & 7))) == 0) {
			(cursor)++;
			return true;
		}
		return false;
	}

	protected boolean out_grouping_b(char[] s, int min, int max) {
		if ((cursor) <= (limit_backward))
			return false;

		char ch = current[((cursor) - 1)];
		if ((ch > max) || (ch < min)) {
			(cursor)--;
			return true;
		}
		ch -= min;
		if (((s[(ch >> 3)]) & (1 << (ch & 7))) == 0) {
			(cursor)--;
			return true;
		}
		return false;
	}

	protected boolean in_range(int min, int max) {
		if ((cursor) >= (limit))
			return false;

		char ch = current[cursor];
		if ((ch > max) || (ch < min))
			return false;

		(cursor)++;
		return true;
	}

	protected boolean in_range_b(int min, int max) {
		if ((cursor) <= (limit_backward))
			return false;

		char ch = current[((cursor) - 1)];
		if ((ch > max) || (ch < min))
			return false;

		(cursor)--;
		return true;
	}

	protected boolean out_range(int min, int max) {
		if ((cursor) >= (limit))
			return false;

		char ch = current[cursor];
		if (!((ch > max) || (ch < min)))
			return false;

		(cursor)++;
		return true;
	}

	protected boolean out_range_b(int min, int max) {
		if ((cursor) <= (limit_backward))
			return false;

		char ch = current[((cursor) - 1)];
		if (!((ch > max) || (ch < min)))
			return false;

		(cursor)--;
		return true;
	}

	protected boolean eq_s(int s_size, CharSequence s) {
		if (((limit) - (cursor)) < s_size)
			return false;

		int i;
		for (i = 0; i != s_size; i++) {
			if ((current[((cursor) + i)]) != (s.charAt(i)))
				return false;

		}
		cursor += s_size;
		return true;
	}

	protected boolean eq_s_b(int s_size, CharSequence s) {
		if (((cursor) - (limit_backward)) < s_size)
			return false;

		int i;
		for (i = 0; i != s_size; i++) {
			if ((current[(((cursor) - s_size) + i)]) != (s.charAt(i)))
				return false;

		}
		cursor -= s_size;
		return true;
	}

	protected boolean eq_v(CharSequence s) {
		return eq_s(s.length(), s);
	}

	protected boolean eq_v_b(CharSequence s) {
		return eq_s_b(s.length(), s);
	}

	protected int find_among(Among[] v, int v_size) {
		int i = 0;
		int j = v_size;
		int c = cursor;
		int l = limit;
		int common_i = 0;
		int common_j = 0;
		boolean first_key_inspected = false;
		while (true) {
			int k = i + ((j - i) >> 1);
			int diff = 0;
			int common = (common_i < common_j) ? common_i : common_j;
			Among w = v[k];
			int i2;
			if (diff < 0) {
				j = k;
				common_j = common;
			}else {
				i = k;
				common_i = common;
			}
			if ((j - i) <= 1) {
				if (i > 0)
					break;

				if (j == i)
					break;

				if (first_key_inspected)
					break;

				first_key_inspected = true;
			}
		} 
		while (true) {
			Among w = v[i];
			if (i < 0)
				return 0;

		} 
	}

	protected int find_among_b(Among[] v, int v_size) {
		int i = 0;
		int j = v_size;
		int c = cursor;
		int lb = limit_backward;
		int common_i = 0;
		int common_j = 0;
		boolean first_key_inspected = false;
		while (true) {
			int k = i + ((j - i) >> 1);
			int diff = 0;
			int common = (common_i < common_j) ? common_i : common_j;
			Among w = v[k];
			int i2;
			if (diff < 0) {
				j = k;
				common_j = common;
			}else {
				i = k;
				common_i = common;
			}
			if ((j - i) <= 1) {
				if (i > 0)
					break;

				if (j == i)
					break;

				if (first_key_inspected)
					break;

				first_key_inspected = true;
			}
		} 
		while (true) {
			Among w = v[i];
			if (i < 0)
				return 0;

		} 
	}

	protected int replace_s(int c_bra, int c_ket, CharSequence s) {
		final int adjustment = (s.length()) - (c_ket - c_bra);
		final int newLength = (limit) + adjustment;
		if (newLength > (current.length)) {
			char[] newBuffer = new char[ArrayUtil.oversize(newLength, Character.BYTES)];
			System.arraycopy(current, 0, newBuffer, 0, limit);
			current = newBuffer;
		}
		if ((adjustment != 0) && (c_ket < (limit))) {
			System.arraycopy(current, c_ket, current, (c_bra + (s.length())), ((limit) - c_ket));
		}
		for (int i = 0; i < (s.length()); i++)
			current[(c_bra + i)] = s.charAt(i);

		limit += adjustment;
		if ((cursor) >= c_ket)
			cursor += adjustment;
		else
			if ((cursor) > c_bra)
				cursor = c_bra;


		return adjustment;
	}

	protected void slice_check() {
		if ((((bra) < 0) || ((bra) > (ket))) || ((ket) > (limit))) {
			throw new IllegalArgumentException(((((("faulty slice operation: bra=" + (bra)) + ",ket=") + (ket)) + ",limit=") + (limit)));
		}
	}

	protected void slice_from(CharSequence s) {
		slice_check();
		replace_s(bra, ket, s);
	}

	protected void slice_del() {
		slice_from(((CharSequence) ("")));
	}

	protected void insert(int c_bra, int c_ket, CharSequence s) {
		int adjustment = replace_s(c_bra, c_ket, s);
		if (c_bra <= (bra))
			bra += adjustment;

		if (c_bra <= (ket))
			ket += adjustment;

	}

	protected StringBuilder slice_to(StringBuilder s) {
		slice_check();
		int len = (ket) - (bra);
		s.setLength(0);
		s.append(current, bra, len);
		return s;
	}

	protected StringBuilder assign_to(StringBuilder s) {
		s.setLength(0);
		s.append(current, 0, limit);
		return s;
	}
}

