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