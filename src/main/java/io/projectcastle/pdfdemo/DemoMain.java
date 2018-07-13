/** ========================================================================= *
 * Copyright (C)  2017, 2018 Salesforce Inc ( http://www.salesforce.com/      *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <swissel@salesforce.com>              *
 *                                       @notessensei                         *
 * @version     1.0                                                           *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== *
 */
package io.projectcastle.pdfdemo;

import java.io.File;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.Pump;

/**
 * @author swissel
 *
 */
public class DemoMain extends AbstractVerticle {

    /**
     * @param args
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        Runner.runVerticle(DemoMain.class.getName(), true);
    }

    @Override
    public void start() throws Exception {
        this.vertx.createHttpServer().requestHandler(req -> {
            if (req.uri().equals("/")) {
                // Serve the index page
                req.response().sendFile("index.html");
            } else if (req.uri().startsWith("/form")) {
                req.pause();
                String filename = UUID.randomUUID() + ".uploaded";
                vertx.fileSystem().open(filename, new OpenOptions(), ares -> {
                  AsyncFile file = ares.result();
                  Pump pump = Pump.pump(req, file);
                  req.endHandler(v1 -> file.close(v2 -> {
                      String plaintText = this.file2Text(filename);
                      req.response().setChunked(true).putHeader("Content-Type", "text/plain").end(plaintText);
                    System.out.println("Uploaded to " + filename);
                  }));
                  pump.start();
                  req.resume();

                });
            } else {
                req.response().setStatusCode(404);
                req.response().end();
            }
        }).listen(Integer.valueOf(System.getenv("PORT")));

    }

    private String file2Text(final String fileName) {
        String parsedText = null;
        PDDocument pdDoc = null;
        PDFTextStripper pdfStripper;
        final File source = new File(fileName);
        if (!source.exists()) {
            return (fileName + " doesn't exist");
        }

        try {
            pdfStripper = new PDFTextStripper();
            pdDoc = PDDocument.load(source);

            parsedText = pdfStripper.getText(pdDoc);

        } catch (final Exception e) {
            e.printStackTrace();
            try {
                if (pdDoc != null) {
                    pdDoc.close();
                }
            } catch (final Exception e1) {
                e.printStackTrace();
            }

        }

        return parsedText;
    }

}
