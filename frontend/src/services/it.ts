import { http } from './http'
export interface ItOverview {totalAlerts:number;criticalAlerts:number;unresolvedAlerts:number;failedLogins:number;deniedAccess:number;highRiskUsers:number;activeSessions:number;model:{available:boolean;model_version?:string;algorithm?:string;feature_schema_version?:string;loaded_at?:string}}
export async function getItOverview(){return(await http.get<ItOverview>('/it/overview')).data}
