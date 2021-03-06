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

package com.github.explainable.sql.converter;

import com.github.explainable.sql.ast.expression.SqlColumnReference;
import com.github.explainable.sql.ast.expression.SqlExpression;
import com.github.explainable.sql.ast.select.SqlSelectAllColumns;
import com.github.explainable.sql.ast.select.SqlSelectAllColumnsInTable;
import com.github.explainable.sql.ast.select.SqlSelectColumn;
import com.github.explainable.sql.ast.select.SqlSelectItem;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitor;

import javax.annotation.Nullable;

import static com.github.explainable.sql.converter.ConverterUtils.checkUnsupportedFeature;

final class SelectItemConverter implements SelectItemVisitor {
	private final MasterConverter master;

	@Nullable
	private SqlSelectItem result;

	SelectItemConverter(MasterConverter master) {
		this.master = master;
		this.result = null;
	}

	@Nullable
	public SqlSelectItem convert(SelectItem selectItem) {
		// This silly dance is necessary to ensure that none of the accept(...) methods forget to
		// set the "result" variable. The code would be so much cleaner if accept(...) had a non-void
		// return type. Sigh.
		result = null;
		selectItem.accept(this);
		Preconditions.checkNotNull(result);
		SqlSelectItem realResult = result;
		result = null;
		return realResult;
	}

	@Override
	public void visit(AllColumns allColumns) {
		result = new SqlSelectAllColumns();
	}

	@Override
	public void visit(AllTableColumns allTableColumns) {
		checkUnsupportedFeature(allTableColumns.getTable().getAlias(), "SELECT item table alias");
		checkUnsupportedFeature(allTableColumns.getTable().getPivot(), "SELECT item table pivot");
		checkUnsupportedFeature(allTableColumns.getTable().getSchemaName(), "Schema Name");

		result = new SqlSelectAllColumnsInTable(allTableColumns.getTable().getName());
	}

	@Override
	public void visit(SelectExpressionItem item) {
		SqlExpression child = master.convert(item.getExpression());
		String alias = item.getAlias();

		if (alias == null && child instanceof SqlColumnReference) {
			alias = ((SqlColumnReference) child).columnName();
		}

		result = new SqlSelectColumn(child, alias);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("result", result)
				.toString();
	}
}
