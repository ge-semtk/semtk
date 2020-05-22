package com.ge.research.semtk.git;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class GitRepoHandler {

	SshSessionFactory sshSessionFactory = null;
	TransportConfigCallback transportConfigCallback  = null;
	
	public GitRepoHandler(String privateKeyFilename, String knownHostsFilename) {
		super();

		// -------- Magic to get ssh key and known_host --------
		// https://www.codeaffine.com/2014/12/09/jgit-authentication/
		this.sshSessionFactory = new JschConfigSessionFactory() {
			@Override
			protected void configure( Host host, Session session ) {
				// works but security problem
				// session.setConfig("StrictHostKeyChecking", "no");

			}
			@Override
			protected JSch createDefaultJSch( FS fs ) throws JSchException {
				JSch defaultJSch = super.createDefaultJSch( fs );

				// locate ssh key that will log me into the repo
				defaultJSch.addIdentity( privateKeyFilename );

				// stops the HostKey has been changed error
				// generated inside a gitbash script on my pc:
				// % ssh-keyscan github.build.ge.com > known_hosts
				defaultJSch.setKnownHosts( knownHostsFilename );

				return defaultJSch;
			}
		};

		this.transportConfigCallback = new TransportConfigCallback() {
			@Override
			public void configure( Transport transport ) {
				SshTransport sshTransport = ( SshTransport )transport;
				sshTransport.setSshSessionFactory( sshSessionFactory );
			}
		};
		// ------ end of magic ---------
	}

	public void clone(String repo, File localRepo, String branch) throws InvalidRemoteException, TransportException, GitAPIException {
		CloneCommand cloneCommand = Git.cloneRepository();
		cloneCommand.setTransportConfigCallback( this.transportConfigCallback);
		
		Git git = null;
		try {
			git = cloneCommand
			.setURI( repo )
			.setDirectory(localRepo )
			.setBranch(branch)
			.call();
		} finally {
			git.close();
		}
	}
	
	public void pull(File localRepo, String branch) throws Exception {
		// else pull latest
		Git git = null;
		try {
			git = Git.open(localRepo);
			PullCommand pullCommand = git.pull();
			pullCommand
			.setTransportConfigCallback( this.transportConfigCallback)
			.setRemoteBranchName(branch);

			PullResult pullRes = pullCommand.call();
			if (! pullRes.isSuccessful()) {
				throw new Exception(pullRes.toString());
			}
		} finally {
			git.close();
		}
	}
}
