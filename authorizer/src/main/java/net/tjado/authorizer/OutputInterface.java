/**
 * Authorizer
 *
 *  Copyright 2016 by Tjado Mäcke <tjado@maecke.de>
 *  Licensed under GNU General Public License 3.0.
 *
 * @license GPL-3.0 <https://opensource.org/licenses/GPL-3.0>
 */

package net.tjado.authorizer;

public interface OutputInterface {
    public enum Language { en_US, en_GB, de_DE, AppleMac_de_DE, de_CH, fr_CH, neo }
    public boolean setLanguage(OutputInterface.Language lang);
    public int sendText(String text) throws Exception;
    public int sendReturn() throws Exception;
    public int sendTabulator() throws Exception;
    public void destruct() throws Exception;
}
