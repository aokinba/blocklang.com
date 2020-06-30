export enum ValidateStatus {
	UNVALIDATED,
	VALID,
	INVALID,
}

export enum ResourceType {
	Project = '01',
	Page = '02',
	Group = '03',
	Pane = '04',
	PageTemplet = '05',
	File = '06',
	Service = '07',
	Dependence = '08',
}

// 因为 AppType 主要用于 Page，所以在名称前加上 Page
// 避免与 interfaces.d.ts 中的 AppType 重名
export enum PageAppType {
	Web = '01',
	Mobile = '02',
	MiniProgram = '03',
	// Android = '02',
	// iOS = '03',
	// WechatMiniApp = '04',
	// AlipayMiniApp = '05',
	// QuickApp = '06',
	Unknown = '99',
}

export enum GitFileStatus {
	Untracked = '01',
	Added = '02',
	Changed = '03',
	Removed = '04',
	Deleted = '05',
	Missing = '06',
	Modified = '07',
	Conflicting = '08',
}

export enum ReleaseResult {
	Inited = '01',
	Started = '02',
	Failed = '03',
	Passed = '04',
	Canceled = '05',
}

export enum PublishType {
	New = '01',
	Upgrade = '02',
}

export enum AccessLevel {
	Forbidden = '01',
	Read = '02',
	Write = '03',
	Admin = '04',
}

export enum ProgrammingLanguage {
	Java = '01',
	TypeScript = '02',
}

export enum RepoCategory {
	Widget = '01',
	ClientApi = '02',
	ServerApi = '03',
}
