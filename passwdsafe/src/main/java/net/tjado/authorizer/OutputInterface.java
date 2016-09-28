/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.authorizer;

/**
 * Created by tm on 21.04.16.
 */
public interface OutputInterface {

    public enum Language { en_US, de_DE};
    public boolean setLanguage(OutputInterface.Language lang);
    public void sendText(String text) throws Exception;
    public void destruct() throws Exception;
}
