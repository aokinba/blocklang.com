import ThemedMixin, { theme } from '@dojo/framework/core/mixins/Themed';
import I18nMixin from '@dojo/framework/core/mixins/I18n';
import WidgetBase from '@dojo/framework/core/WidgetBase';

import messageBundle from '../../../nls/main';

import * as c from '@blocklang/bootstrap-classes';
import * as css from './NewGroup.m.css';
import { v, w } from '@dojo/framework/core/vdom';
import Exception from '../../error/Exception';
import { Repository, WithTarget, RepositoryResourceGroup, Errors } from '../../../interfaces';
import RepositoryHeader from '../../widgets/RepositoryHeader';
import Link from '@dojo/framework/routing/Link';
import { ValidateStatus } from '../../../constant';
import { DescriptionPayload, GroupKeyPayload, GroupNamePayload } from '../../../processes/interfaces';
import { canNewGroup } from '../../../permission';

export interface NewGroupProperties {
	// user
	loggedUsername: string;
	repository: Repository;
	// attr
	parentId: number; // 所属分组标识
	parentGroups: RepositoryResourceGroup[];
	// validation
	keyValidateStatus?: ValidateStatus;
	keyErrorMessage?: string;
	nameValidateStatus?: ValidateStatus;
	nameErrorMessage?: string;
	saveFailedErrors: Errors;
	// event
	onKeyInput: (opts: GroupKeyPayload) => void;
	onNameInput: (opts: GroupNamePayload) => void;
	onDescriptionInput: (opts: DescriptionPayload) => void;
	onSaveGroup: (opts: object) => void;
}

@theme(css)
export default class NewGroup extends ThemedMixin(I18nMixin(WidgetBase))<NewGroupProperties> {
	private _localizedMessages = this.localizeBundle(messageBundle);

	protected render() {
		// TODO: 只有授权用户才能访问，还需要判断用户对项目是否有写的权限
		if (!this._isAuthenticated()) {
			return w(Exception, { type: '403' });
		}

		const {
			messages: { newGroup },
		} = this._localizedMessages;

		const { saveFailedErrors } = this.properties;

		return v('div', { classes: [css.root, c.container] }, [
			saveFailedErrors
				? v('div', { classes: [c.alert, c.alert_danger, c.alert_dismissible], role: 'alert' }, [
						v('h4', { classes: [c.alert_heading] }, ['保存失败！']),
						v('p', {}, [`${saveFailedErrors.global}`]),
						v(
							'button',
							{ type: 'button', classes: [c.close], 'data-dismiss': 'alert', 'aria-label': 'Close' },
							[v('span', { 'aria-hidden': 'true', innerHTML: '&times;' }, [])]
						),
				  ])
				: undefined,
			this._renderHeader(),
			v('div', { classes: [c.container], styles: { maxWidth: '700px' } }, [
				v('div', [
					v('h4', [`${newGroup}`]),
					this._renderBreadcrumb(),
					v('hr'),
					v('form', { classes: [c.needs_validation], novalidate: 'novalidate' }, [
						this._renderKeyInput(),
						this._renderNameInput(),
						this._renderDescriptionTextarea(),
						v('hr'),
						this._renderButtons(),
					]),
				]),
			]),
		]);
	}

	private _isAuthenticated() {
		const { repository, loggedUsername } = this.properties;
		const isLogin = !!loggedUsername;
		if (!isLogin) {
			return false;
		}
		return canNewGroup(repository.accessLevel);
	}

	private _renderHeader() {
		const {
			messages: { privateRepositoryTitle },
		} = this._localizedMessages;
		const { repository } = this.properties;

		return w(RepositoryHeader, { repository, privateRepositoryTitle });
	}

	private _renderBreadcrumb() {
		const { repository, parentGroups = [] } = this.properties;

		return v('nav', { classes: [], 'aria-label': 'breadcrumb' }, [
			v('ol', { classes: [c.breadcrumb, css.navOl] }, [
				// 项目名
				v('li', { classes: [c.breadcrumb_item] }, [
					w(
						Link,
						{
							to: 'view-repo',
							params: { owner: repository.createUserName, repo: repository.name },
							classes: [c.font_weight_bold],
						},
						[`${repository.name}`]
					),
				]),
				...parentGroups.map((item) => {
					return v('li', { classes: [c.breadcrumb_item] }, [
						w(
							Link,
							{
								to: 'view-repo-group',
								params: {
									owner: repository.createUserName,
									repo: repository.name,
									parentPath: item.path.substring(1),
								},
							},
							[`${item.name}`]
						),
					]);
				}),
			]),
		]);
	}

	private _renderKeyInput() {
		const {
			messages: { groupKeyLabel, groupKeyHelp, requiredLabel },
		} = this._localizedMessages;

		const { keyValidateStatus = ValidateStatus.UNVALIDATED, keyErrorMessage } = this.properties;

		const inputClasses = [c.form_control];
		if (keyValidateStatus === ValidateStatus.INVALID) {
			inputClasses.push(c.is_invalid);
		}

		return v('div', { classes: [c.form_group] }, [
			v('label', { for: 'key' }, [
				`${groupKeyLabel}`,
				v('small', { classes: [c.text_muted] }, [`${requiredLabel}`]),
			]),
			v('div', { classes: [c.input_group] }, [
				v('input', {
					type: 'text',
					id: 'key',
					classes: inputClasses,
					required: true,
					focus: true,
					maxlength: 32,
					oninput: this._onKeyInput,
				}),
				// 当校验未通过时显示
				keyValidateStatus === ValidateStatus.INVALID
					? v('div', { classes: [c.invalid_tooltip], innerHTML: `${keyErrorMessage}` })
					: null,
			]),
			v('small', { classes: [c.form_text, c.text_muted] }, [`${groupKeyHelp}`]),
		]);
	}

	private _renderNameInput() {
		const {
			messages: { groupNameLabel, groupNameHelp },
		} = this._localizedMessages;

		const { nameValidateStatus = ValidateStatus.UNVALIDATED, nameErrorMessage } = this.properties;

		const inputClasses = [c.form_control];
		if (nameValidateStatus === ValidateStatus.INVALID) {
			inputClasses.push(c.is_invalid);
		}

		return v('div', { classes: [c.form_group] }, [
			v('label', { for: 'name' }, [`${groupNameLabel}`]),
			v('div', { classes: [c.input_group] }, [
				v('input', {
					type: 'text',
					id: 'name',
					classes: inputClasses,
					maxlength: 32,
					oninput: this._onNameInput,
				}),
				// 当校验未通过时显示
				nameValidateStatus === ValidateStatus.INVALID
					? v('div', { classes: [c.invalid_tooltip], innerHTML: `${nameErrorMessage}` })
					: null,
			]),
			v('small', { classes: [c.form_text, c.text_muted] }, [`${groupNameHelp}`]),
		]);
	}

	private _renderDescriptionTextarea() {
		const {
			messages: { pageDescriptionLabel },
		} = this._localizedMessages;

		return v('div', { classes: [c.form_group] }, [
			v('label', { for: 'description' }, [`${pageDescriptionLabel}`]),
			v('textarea', {
				classes: [c.form_control],
				rows: 2,
				id: 'description',
				maxlength: 64,
				oninput: this._onDescriptionInput,
			}),
		]);
	}

	private _renderButtons() {
		const {
			messages: { groupSaveLabel, groupCancelSaveLabel },
		} = this._localizedMessages;

		const {
			repository,
			parentGroups = [],
			keyValidateStatus = ValidateStatus.UNVALIDATED,
			nameValidateStatus = ValidateStatus.UNVALIDATED,
		} = this.properties;
		// name 默认可以为空，所以可以不用填写，即不走校验。
		const disabled =
			keyValidateStatus === ValidateStatus.VALID && nameValidateStatus !== ValidateStatus.INVALID ? false : true;

		return v('div', [
			v(
				'button',
				{
					type: 'button',
					classes: [c.btn, c.btn_primary],
					disabled,
					onclick: disabled ? undefined : this._onSaveGroup,
				},
				[`${groupSaveLabel}`]
			),
			' ',
			parentGroups.length === 0
				? w(
						Link,
						{
							classes: [c.btn, c.btn_secondary],
							to: 'view-repo',
							params: { owner: repository.createUserName, repo: repository.name },
						},
						[`${groupCancelSaveLabel}`]
				  )
				: w(
						Link,
						{
							classes: [c.btn, c.btn_secondary],
							to: 'view-repo-group',
							params: {
								owner: repository.createUserName,
								repo: repository.name,
								parentPath: parentGroups[parentGroups.length - 1].path.substring(1),
							},
						},
						[`${groupCancelSaveLabel}`]
				  ),
		]);
	}

	private _onKeyInput({ target: { value: key } }: WithTarget) {
		const {
			repository: { createUserName, name },
			parentId,
		} = this.properties;
		this.properties.onKeyInput({ key, owner: createUserName, repo: name, parentId });
	}

	private _onNameInput({ target: { value: pageName } }: WithTarget) {
		const {
			repository: { createUserName, name },
			parentId,
		} = this.properties;
		this.properties.onNameInput({ name: pageName, owner: createUserName, repo: name, parentId });
	}

	private _onDescriptionInput({ target: { value: description } }: WithTarget) {
		this.properties.onDescriptionInput({ description });
	}

	private _onSaveGroup() {
		const {
			repository: { createUserName, name },
			parentGroups,
		} = this.properties;

		let parentPath = '';
		if (parentGroups.length > 0) {
			parentPath = parentGroups[parentGroups.length - 1].path.substring(1);
		}
		this.properties.onSaveGroup({ owner: createUserName, project: name, parentPath });
	}
}
