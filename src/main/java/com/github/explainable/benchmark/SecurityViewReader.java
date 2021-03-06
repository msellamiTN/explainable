/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Gabriel Bender
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.explainable.benchmark;

import com.github.explainable.corelang.View;
import com.github.explainable.sql.Schema;
import com.github.explainable.sql.pipeline.passes.ViewExtractionPipeline;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.select.Select;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;

/**
 * Utility class for reading in and parsing security views that are defined as SQL queries in a file
 * on disk.
 */
public final class SecurityViewReader {
	private final BufferedReader reader;

	private final CCJSqlParserManager parserManager;

	private final ViewExtractionPipeline pipeline;

	SecurityViewReader(InputStream stream, Schema schema) {
		Preconditions.checkNotNull(stream);
		this.reader = new BufferedReader(new InputStreamReader(stream));
		this.parserManager = new CCJSqlParserManager();
		this.pipeline = ViewExtractionPipeline.create(schema);
	}

	public String readSqlView() throws IOException {
		StringBuilder builder = new StringBuilder();
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				// EOF
				break;
			}

			line = line.trim();
			if (line.startsWith("--")) {
				// It's a comment
				continue;
			}

			builder.append(System.getProperty("line.separator")).append(line);

			if (line.endsWith(";")) {
				// End of the expression
				break;
			}
		}

		String sql = builder.toString().trim();
		if (sql.isEmpty()) {
			return null;
		}

		return sql;
	}

	public View readView() throws IOException, JSQLParserException {
		String sql = readSqlView();
		if (sql == null) {
			return null;
		}
		return pipeline.execute((Select) parserManager.parse(new StringReader(sql))).get(0);
	}

	public void close() throws IOException {
		reader.close();
	}

	/**
	 * Utility method that opens a file, reads all the views in the file, and then closes it and
	 * returns the resulting set of views.
	 *
	 * @param file the file to read from
	 */
	public static List<View> readViews(File file, Schema schema)
			throws IOException, JSQLParserException {
		SecurityViewReader reader = new SecurityViewReader(new FileInputStream(file), schema);
		List<View> views = Lists.newArrayList();

		while (true) {
			View view = reader.readView();
			if (view == null) {
				break;
			}
			views.add(view);
		}

		reader.close();
		return views;
	}

	public static List<String> readSqlViews(File file, Schema schema)
			throws IOException {
		SecurityViewReader reader = new SecurityViewReader(new FileInputStream(file), schema);
		List<String> views = Lists.newArrayList();

		while (true) {
			String sqlView = reader.readSqlView();
			if (sqlView == null) {
				break;
			}
			views.add(sqlView);
		}

		reader.close();
		return views;
	}
}
