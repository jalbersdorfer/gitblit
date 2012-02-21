/*
 * Copyright 2011 gitblit.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitblit.wicket.panels;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.StringResourceModel;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.gitblit.Constants;
import com.gitblit.GitBlit;
import com.gitblit.Keys;
import com.gitblit.models.RefModel;
import com.gitblit.utils.JGitUtils;
import com.gitblit.utils.StringUtils;
import com.gitblit.wicket.WicketUtils;
import com.gitblit.wicket.pages.CommitDiffPage;
import com.gitblit.wicket.pages.CommitPage;
import com.gitblit.wicket.pages.LogPage;
import com.gitblit.wicket.pages.SearchPage;
import com.gitblit.wicket.pages.SummaryPage;
import com.gitblit.wicket.pages.TreePage;

public class LogPanel extends BasePanel {

	private static final long serialVersionUID = 1L;

	private boolean hasMore;

	public LogPanel(String wicketId, final String repositoryName, final String objectId,
			Repository r, int limit, int pageOffset) {
		super(wicketId);
		boolean pageResults = limit <= 0;
		int itemsPerPage = GitBlit.getInteger(Keys.web.itemsPerPage, 50);
		if (itemsPerPage <= 1) {
			itemsPerPage = 50;
		}

		final Map<ObjectId, List<RefModel>> allRefs = JGitUtils.getAllRefs(r);
		List<RevCommit> commits;
		if (pageResults) {
			// Paging result set
			commits = JGitUtils.getRevLog(r, objectId, pageOffset * itemsPerPage, itemsPerPage);
		} else {
			// Fixed size result set
			commits = JGitUtils.getRevLog(r, objectId, 0, limit);
		}

		// inaccurate way to determine if there are more commits.
		// works unless commits.size() represents the exact end.
		hasMore = commits.size() >= itemsPerPage;

		// header
		if (pageResults) {
			// shortlog page
			// show repository summary page link
			add(new LinkPanel("header", "title", objectId, SummaryPage.class,
					WicketUtils.newRepositoryParameter(repositoryName)));
		} else {
			// summary page
			// show shortlog page link
			add(new LinkPanel("header", "title", objectId, LogPage.class,
					WicketUtils.newRepositoryParameter(repositoryName)));
		}

		ListDataProvider<RevCommit> dp = new ListDataProvider<RevCommit>(commits);
		DataView<RevCommit> logView = new DataView<RevCommit>("commit", dp) {
			private static final long serialVersionUID = 1L;
			int counter;

			public void populateItem(final Item<RevCommit> item) {
				final RevCommit entry = item.getModelObject();
				final Date date = JGitUtils.getCommitDate(entry);

				item.add(WicketUtils.createDateLabel("commitDate", date, getTimeZone()));

				// author search link
				String author = entry.getAuthorIdent().getName();
				LinkPanel authorLink = new LinkPanel("commitAuthor", "list", author,
						SearchPage.class, WicketUtils.newSearchParameter(repositoryName, objectId,
								author, Constants.SearchType.AUTHOR));
				setPersonSearchTooltip(authorLink, author, Constants.SearchType.AUTHOR);
				item.add(authorLink);

				// merge icon
				if (entry.getParentCount() > 1) {
					item.add(WicketUtils.newImage("commitIcon", "commit_merge_16x16.png"));
				} else {
					item.add(WicketUtils.newBlankImage("commitIcon"));
				}

				// short message
				String shortMessage = entry.getShortMessage();
				String trimmedMessage = StringUtils.trimShortLog(shortMessage);
				LinkPanel shortlog = new LinkPanel("commitShortMessage", "list subject",
						trimmedMessage, CommitPage.class, WicketUtils.newObjectParameter(
								repositoryName, entry.getName()));
				if (!shortMessage.equals(trimmedMessage)) {
					WicketUtils.setHtmlTooltip(shortlog, shortMessage);
				}
				item.add(shortlog);

				item.add(new RefsPanel("commitRefs", repositoryName, entry, allRefs));

				item.add(new BookmarkablePageLink<Void>("view", CommitPage.class, WicketUtils
						.newObjectParameter(repositoryName, entry.getName())));
				item.add(new BookmarkablePageLink<Void>("diff", CommitDiffPage.class, WicketUtils
						.newObjectParameter(repositoryName, entry.getName())).setEnabled(entry
						.getParentCount() > 0));
				item.add(new BookmarkablePageLink<Void>("tree", TreePage.class, WicketUtils
						.newObjectParameter(repositoryName, entry.getName())));

				WicketUtils.setAlternatingBackground(item, counter);
				counter++;
			}
		};
		add(logView);

		// determine to show pager, more, or neither
		if (limit <= 0) {
			// no display limit
			add(new Label("moreLogs", "").setVisible(false));
		} else {
			if (pageResults) {
				// paging
				add(new Label("moreLogs", "").setVisible(false));
			} else {
				// more
				if (commits.size() == limit) {
					// show more
					add(new LinkPanel("moreLogs", "link", new StringResourceModel("gb.moreLogs",
							this, null), LogPage.class,
							WicketUtils.newRepositoryParameter(repositoryName)));
				} else {
					// no more
					add(new Label("moreLogs", "").setVisible(false));
				}
			}
		}
	}

	public boolean hasMore() {
		return hasMore;
	}
}