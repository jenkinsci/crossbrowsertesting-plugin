package org.jenkinsci.plugins.cbt_jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.HostnamePortRequirement;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.crossbrowsertesting.api.Account;
import com.crossbrowsertesting.plugin.Constants;

import hudson.Extension;
import hudson.model.AbstractItem;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.Secret;

@SuppressWarnings("serial")
public class CBTCredentials extends BaseStandardCredentials implements StandardUsernamePasswordCredentials {
	private final String username;
	private final Secret authkey;
    public final static DomainRequirement DOMAIN_REQUIREMENT = new HostnamePortRequirement("crossbrowsertesting.com", 443);
	
	@DataBoundConstructor
	public CBTCredentials(@CheckForNull String username, @CheckForNull String authkey) {
		super(CredentialsScope.GLOBAL, String.valueOf(username.concat(authkey).hashCode()), "");
		this.username = username;
		this.authkey = Secret.fromString(authkey);
	}
	
	@Override
	public String getUsername() {
		return this.username;
	}
	@Override
	public Secret getPassword() {
		return this.authkey;
	}
	public String getAuthkey() {
		return Secret.toString(this.authkey);
	}
    public static List<CBTCredentials> all(ItemGroup context) {
        return CredentialsProvider.lookupCredentials(CBTCredentials.class, context, ACL.SYSTEM, CBTCredentials.DOMAIN_REQUIREMENT);
    }

    public static List<CBTCredentials> all(Item context) {
        return CredentialsProvider.lookupCredentials(CBTCredentials.class, context, ACL.SYSTEM, CBTCredentials.DOMAIN_REQUIREMENT);
    }
    public static CBTCredentials getCredentials(final AbstractItem buildItem, final String credentialsId) {
    	List<CBTCredentials> creds = CredentialsProvider.lookupCredentials(CBTCredentials.class, buildItem, null, new ArrayList<DomainRequirement>());
    	if (creds.isEmpty()) {
            return null;
        }
        CredentialsMatcher matcher;
        if (credentialsId != null) {
            matcher = CredentialsMatchers.allOf(CredentialsMatchers.withId(credentialsId));
        } else {
            matcher = CredentialsMatchers.always();
        }
        return CredentialsMatchers.firstOrDefault(creds, matcher,creds.get(0));
    }
    @Extension
    public static class DescriptorImpl extends CredentialsDescriptor {

        public FormValidation doTestConnection(@QueryParameter("username") final String username, @QueryParameter("authkey") final String authkey) throws IOException, ServletException {
        	Account account = new Account(username, authkey);
        	account.init();
        	if (account.connectionSuccessful) {
        		account.sendMixpanelEvent("Jenkins Plugin Downloaded"); // track install
                return FormValidation.ok("Successful Authentication");
            } else {
                return FormValidation.error("Error: Bad username or authkey");
            }
        }

        @Override
        public String getDisplayName() {
            return Constants.DISPLAYNAME;
        }
 
    }
}
