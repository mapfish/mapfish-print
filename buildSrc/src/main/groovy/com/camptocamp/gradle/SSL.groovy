/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.camptocamp.gradle;

class SSL {
  private def mPrivateKey = System.env['privateKey'] ?: System.properties['privateKey']
  private def mPassphrase = System.env['sshPassphrase'] ?: System.properties['sshPassphrase'] ?: System.env['sslPassphrase'] ?: System.properties['sslPassphrase']
  private def passphraseWarned, privateKeyWarned  = false;
  
  def privateKey() {
    
    if(mPrivateKey==null) {
      def defaultDir = new File(System.properties['user.home'], '.ssh')
      def key = defaultDir.listFiles().find {it.name == 'id_rsa' || it.name == 'id_dsa'}
      if(key!=null) {
        mPrivateKey = key.path
      } else if(!privateKeyWarned) {
        privateKeyWarned = true
        System.err.println("[WARNING] Private key was not found.  The 'privateKey' property is referenced as well as the ~/.ssh directory for the key file")
      }
    }
    return mPrivateKey  
  }

  def passphrase() {
    if(!passphraseWarned && mPassphrase==null) {
      passphraseWarned = true
      System.err.println("[WARNING] SSL passphrase was not found.  Set '-DsshPassphrase=<password>' property on gradle launch")
    }
    return mPassphrase  
  }
}