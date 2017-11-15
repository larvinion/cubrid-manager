/*
 * Copyright (C) 2009 Search Solution Corporation. All rights reserved by Search Solution.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of the <ORGANIZATION> nor the names of its contributors
 *   may be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 */
package com.cubrid.common.ui.cubrid.table.importhandler.handler;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;

//import com.cubrid.common.core.reader.SQLReader;
import com.cubrid.common.core.util.LogUtil;
import com.cubrid.common.ui.cubrid.table.importhandler.ImportFileDescription;
import com.cubrid.common.ui.cubrid.table.importhandler.ImportFileHandler;

/**
 * The SQL Import File Handler
 *
 * @author SeungHyun
 * @version 0.1 - 2017-11-15 2017 created by SeungHyun
 */
public class SQLImportFileHandler implements
		ImportFileHandler {
	
	private static final Logger LOGGER = LogUtil.getLogger(SQLImportFileHandler.class);
	
	private final String fileName;
	private final String fileCharset;

	public SQLImportFileHandler(String fileName, String fileCharset) {
		super();
		this.fileName = fileName;
		this.fileCharset = fileCharset;
	}

	/**
	 * Get the source file information
	 *
	 * @return ImportFileDescription
	 * @throws Exception in process.
	 */
	public ImportFileDescription getSourceFileInfo() throws Exception {
		final List<String> colsList = new ArrayList<String>();
		final List<Integer> itemsNumberOfSheets = new ArrayList<Integer>();

		final ImportFileDescription importFileDescription = new ImportFileDescription(
				0, 1, colsList);
		importFileDescription.setItemsNumberOfSheets(itemsNumberOfSheets);

		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(final IProgressMonitor monitor) {
				monitor.beginTask("", IProgressMonitor.UNKNOWN);
				int totalRowCount = 0;
				SQLReader sqlReader = null;
				try {
					if (fileCharset == null || fileCharset.trim().length() == 0) {
						sqlReader = new SQLReader(new FileReader(fileName));
					} else {
						sqlReader = new SQLReader(new InputStreamReader(
								new FileInputStream(fileName), fileCharset));
					}

					String[] sqlRow = sqlReader.readNextRow();
					if (sqlRow != null) {
						totalRowCount++;
						for (String title : sqlRow) {
							colsList.add(title);
						}
					}
					while (!monitor.isCanceled()
							&& sqlReader.readNextRow() != null) {
						totalRowCount++;
					}
					itemsNumberOfSheets.add(Integer.valueOf(totalRowCount));
					if (monitor.isCanceled()) {
						throw new InterruptedException();
					}
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
					throw new RuntimeException(e);
				} finally {
					importFileDescription.setTotalCount(totalRowCount);
					importFileDescription.setFirstRowCols(colsList);
					importFileDescription.setItemsNumberOfSheets(itemsNumberOfSheets);
					closeFile(sqlReader);
					monitor.done();
				}
			}
		};
		PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);

		return importFileDescription;
	}
	
	/**
	 * Close SQL file
	 *
	 * @param sqlReader the read need to be closed.
	 */
	private void closeFile(SQLReader sqlReader) { // FIXME move this logic to core module
		if (sqlReader != null) {
			try {
				sqlReader.close();
			} catch (IOException e) {
				LOGGER.error(e.getMessage());
			}
		}
	}

}
