package net.opentechnology.triki.mtd.pages;

import static net.opentechnology.triki.mtd.security.HmrcIdentityProvider.HMRC_TOKEN;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import net.opentechnology.triki.auth.resources.SessionUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class MtdLogin extends MtdVatParent {

  @SpringBean
  private SessionUtils sessionUtils;

  private final LoginStep authenticateStep;
  private final LoginStep authoriseStep;
  private final LoginStep manageVatStep;

  public MtdLogin() {
    // Initialise session if not already created
    HttpSession session =  ((HttpServletRequest) getRequest().getContainerRequest()).getSession();

    authenticateStep = new LoginStep("authenticateStep");
    add(authenticateStep);
    authoriseStep = new LoginStep("authoriseStep");
    add(authoriseStep);
    manageVatStep = new LoginStep("manageVatStep");
    add(manageVatStep);
  }

  @Override
  protected void onConfigure() {
    super.onConfigure();

    Fragment loginButtonsFragment = new  Fragment ("loginButtons", "authenticateButtons", this);
    add(loginButtonsFragment);

    if(sessionUtils.hasAuthenticatedEmail()){
      authenticateStep.setEnabledMode(false, "lock");
      authenticateStep.add(new AttributeAppender("class", Model.of(" disabled")));

      if(sessionUtils.hasModuleToken(HMRC_TOKEN)){
        authoriseStep.setEnabledMode(false, "lock");
        authoriseStep.add(new AttributeAppender("class", Model.of(" disabled")));

        manageVatStep.setEnabledMode(true, "keyboard");
        manageVatStep.add(new AttributeAppender("class", Model.of(" active")));
        Fragment manageVatIntroFragment = new  Fragment ("loginButtons", "manageVatIntro", this);
        replace(manageVatIntroFragment);
      }
      else {
        authoriseStep.setEnabledMode(true, "open lock");
        authoriseStep.add(new AttributeAppender("class", Model.of(" active")));
        Fragment hmrcButtonsFragment = new  Fragment ("loginButtons", "authoriseButtons", this);
        replace(hmrcButtonsFragment);

        manageVatStep.setEnabledMode(false, "keyboard");
        manageVatStep.add(new AttributeAppender("class", Model.of(" disabled")));
      }
    }
    else {
      authenticateStep.setEnabledMode(true, "open lock");
      authenticateStep.add(new AttributeAppender("class", Model.of(" active")));

      authoriseStep.setEnabledMode(false, "open lock");
      authoriseStep.add(new AttributeAppender("class", Model.of(" disabled")));

      manageVatStep.setEnabledMode(false, "keyboard");
      manageVatStep.add(new AttributeAppender("class", Model.of(" disabled")));
    }

  }



}
