import { http } from './http'
import type { PageResponse } from '@/types/organization'

export interface Product { id:string;productCode:string;name:string;categoryId:string;categoryName:string;brandId?:string;brandName?:string;description?:string;warrantyMonths:number;status:string;version:number }
export interface Variant { id:string;productId:string;productName:string;sku:string;barcode?:string;name:string;attributes:Record<string,unknown>;costPrice:number;sellingPrice:number;currency:string;status:string;version:number }
export interface ProductImage {id:string;productId:string;originalFilename:string;mediaType:string;byteSize:number;checksumSha256:string;sortOrder:number;createdAt:string}
export interface Warehouse { id:string;code:string;name:string;address?:string;status:string }
export interface Inventory { id:string;warehouseName:string;variantId:string;sku:string;productName:string;quantity:number;reservedQuantity:number;availableQuantity:number;reorderLevel:number }
export interface StockLedgerEntry{transactionId:string;reference:string;warehouseId:string;warehouseName:string;type:string;actorId:string;sourceType?:string;sourceId?:string;reason?:string;occurredAt:string;variantId:string;sku:string;productName:string;quantityDelta:number;quantityBefore:number;quantityAfter:number;unitCost?:number}
export interface Supplier { id:string;supplierCode:string;name:string;contactName?:string;phone?:string;email?:string;address?:string;status:string;version:number }
export interface SupplierHistory {supplier:Supplier;totalPurchaseOrders:number;receivedValue:number;purchases:PurchaseOrder[]}
export interface PurchaseLine { id:string;variantId:string;sku:string;orderedQuantity:number;receivedQuantity:number;unitCost:number;lineTotal:number }
export interface PurchaseOrder { id:string;poNumber:string;supplierName:string;warehouseName:string;status:string;currency:string;total:number;createdAt:string;items:PurchaseLine[] }

export const getProducts=async(search='')=>(await http.get<PageResponse<Product>>('/products',{params:{search}})).data
export const updateProduct=async(product:Product)=>(await http.put<Product>(`/products/${product.id}`,{productCode:product.productCode,name:product.name,categoryId:product.categoryId,brandId:product.brandId||null,description:product.description||null,warrantyMonths:product.warrantyMonths,status:product.status,version:product.version})).data
export const getVariants=async(productId?:string)=>(await http.get<Variant[]>('/product-variants',{params:{productId}})).data
export const getProductImages=async(productId:string)=>(await http.get<ProductImage[]>(`/products/${productId}/images`)).data
export async function uploadProductImage(productId:string,file:File,sortOrder=0){const body=new FormData();body.append('file',file);return(await http.post<ProductImage>(`/products/${productId}/images`,body,{params:{sortOrder}})).data}
export const getProductImageContent=async(imageId:string)=>(await http.get<Blob>(`/product-images/${imageId}/content`,{responseType:'blob'})).data
export const getWarehouses=async()=>(await http.get<Warehouse[]>('/warehouses')).data
export const createWarehouse=async(input:{code:string;name:string;address?:string;status:string})=>(await http.post<Warehouse>('/warehouses',input)).data
export const getInventory=async(search='',warehouseId?:string)=>(await http.get<PageResponse<Inventory>>('/inventory',{params:{search,warehouseId}})).data
export const getStockLedger=async(warehouseId?:string,variantId?:string,type='')=>(await http.get<PageResponse<StockLedgerEntry>>('/stock-transactions',{params:{warehouseId,variantId,type,size:100}})).data
export const adjustInventory=async(input:{warehouseId:string;variantId:string;quantityDelta:number;reason:string})=>(await http.post('/inventory/adjustments',input)).data
export const getSuppliersPage=async(search='',page=0,size=20,sort='name')=>(await http.get<PageResponse<Supplier>>('/suppliers',{params:{search,page,size,sort}})).data
export const getSuppliers=async()=>(await getSuppliersPage('',0,100)).items
export const searchSuppliers=async(search='',page=0)=>(await getSuppliersPage(search,page)).items
export const createSupplier=async(input:{supplierCode:string;name:string;contactName?:string;phone?:string;email?:string;address?:string;status:string})=>(await http.post<Supplier>('/suppliers',input)).data
export const getSupplierHistory=async(id:string)=>(await http.get<SupplierHistory>(`/suppliers/${id}/history`)).data
export const getPurchasesPage=async(status='',search='',page=0,size=20,sort='createdAt')=>(await http.get<PageResponse<PurchaseOrder>>('/purchase-orders',{params:{status,search,page,size,sort}})).data
export const getPurchases=async(status='')=>(await getPurchasesPage(status,'',0,100)).items
export const submitPurchase=async(id:string)=>(await http.post<PurchaseOrder>(`/purchase-orders/${id}/submit`)).data
export const approvePurchase=async(id:string)=>(await http.post<PurchaseOrder>(`/purchase-orders/${id}/approve`)).data
export const createPurchase=async(input:{supplierId:string;warehouseId:string;currency:string;items:Array<{variantId:string;quantity:number;unitCost:number}>})=>(await http.post<PurchaseOrder>('/purchase-orders',input)).data
export const receivePurchase=async(id:string,input:{idempotencyKey:string;note?:string;items:Array<{purchaseOrderItemId:string;quantity:number}>})=>(await http.post(`/purchase-orders/${id}/receipts`,input)).data
export interface Transfer{ id:string;transferNumber:string;sourceWarehouseId:string;destinationWarehouseId:string;status:string;reason:string;createdBy:string;items:Array<{id:string;variantId:string;sku:string;requestedQuantity:number;dispatchedQuantity:number;receivedQuantity:number}>}
export interface Stocktake{id:string;stocktakeNumber:string;warehouseId:string;status:string;reason:string;createdBy:string;items:Array<{id:string;variantId:string;sku:string;systemQuantity:number;actualQuantity?:number;variance?:number}>}
export const getTransfers=async(page=0,size=20)=>(await http.get<PageResponse<Transfer>>('/stock-transfers',{params:{page,size}})).data
export const createTransfer=async(input:{sourceWarehouseId:string;destinationWarehouseId:string;reason:string;items:Array<{variantId:string;quantity:number}>})=>(await http.post<Transfer>('/stock-transfers',input)).data
export const transferAction=async(id:string,action:'submit'|'approve'|'dispatch'|'receive')=>(await http.post<Transfer>(`/stock-transfers/${id}/${action}`)).data
export const getStocktakes=async(page=0,size=20)=>(await http.get<PageResponse<Stocktake>>('/stocktakes',{params:{page,size}})).data
export const createStocktake=async(input:{warehouseId:string;reason:string})=>(await http.post<Stocktake>('/stocktakes',input)).data
export const countStocktake=async(id:string,items:Array<{itemId:string;actualQuantity:number}>)=>(await http.put<Stocktake>(`/stocktakes/${id}/counts`,{items})).data
export const stocktakeAction=async(id:string,action:'submit'|'approve')=>(await http.post<Stocktake>(`/stocktakes/${id}/${action}`)).data
export interface Customer {id:string;customerCode:string;name:string;phone?:string;email?:string;address?:string;status:string;version:number}
export interface CustomerHistory{customer:Customer;totalOrders:number;totalSpending:number;orders:Order[]}
export interface Order {id:string;orderNumber:string;customerId?:string;warehouseId:string;status:string;currency:string;subtotal:number;discountTotal:number;total:number;paymentStatus:string;invoiceNumber:string;createdAt:string;items:Array<{id:string;variantId:string;sku:string;productName:string;quantity:number;unitPrice:number;lineTotal:number}>}
export const getCustomersPage=async(search='',page=0,size=20,sort='name')=>(await http.get<PageResponse<Customer>>('/customers',{params:{search,page,size,sort}})).data
export const getCustomers=async()=>(await getCustomersPage('',0,100)).items
export const searchCustomers=async(search='',page=0)=>(await getCustomersPage(search,page)).items
export const createCustomer=async(input:{customerCode:string;name:string;phone?:string;email?:string;address?:string;status:string})=>(await http.post<Customer>('/customers',input)).data
export const getCustomerHistory=async(id:string)=>(await http.get<CustomerHistory>(`/customers/${id}/history`)).data
export const getOrdersPage=async(status='',page=0,size=20,sort='createdAt')=>(await http.get<PageResponse<Order>>('/orders',{params:{status,page,size,sort}})).data
export const getOrders=async()=>(await getOrdersPage('',0,100)).items
export const checkout=async(input:{warehouseId:string;customerId?:string;items:Array<{variantId:string;quantity:number}>;discountTotal:number;paymentMethod:string;idempotencyKey:string})=>(await http.post<Order>('/orders/checkout',input)).data
export interface ReturnItem {id:string;orderItemId:string;variantId:string;sku:string;quantity:number;disposition:'RESTOCK'|'DAMAGED'|'QUARANTINE';unitRefund:number;lineRefund:number}
export interface SalesReturn {id:string;returnNumber:string;orderId:string;status:string;reason:string;createdBy:string;approvedBy?:string;approvedAt?:string;completedAt?:string;refundTotal:number;refundStatus?:string;items:ReturnItem[]}
export const getReturns=async(page=0,size=20)=>(await http.get<PageResponse<SalesReturn>>('/returns',{params:{page,size}})).data
export const createReturn=async(input:{orderId:string;reason:string;items:Array<{orderItemId:string;quantity:number;disposition:string}>})=>(await http.post<SalesReturn>('/returns',input)).data
export const submitReturn=async(id:string)=>(await http.post<SalesReturn>(`/returns/${id}/submit`)).data
export const approveReturn=async(id:string)=>(await http.post<SalesReturn>(`/returns/${id}/approve`)).data
export interface Dashboard {summary:{revenue:number;orders:number;averageOrderValue:number;inventoryValue:number;purchaseValue:number};revenueTrend:Array<{date:string;revenue:number;orders:number}>;topProducts:Array<{name:string;value:number}>;revenueByWarehouse:Array<{name:string;value:number}>;revenueByEmployee:Array<{name:string;revenue:number;orders:number}>;purchaseTrend:Array<{date:string;receivedValue:number;receipts:number}>;stockMovements:Array<{type:string;quantity:number}>;lowStock:Array<{sku:string;product:string;warehouse:string;quantity:number;reorderLevel:number}>}
export const getDashboard=async(from:string,to:string)=>(await http.get<Dashboard>('/reports/dashboard',{params:{from,to}})).data
export const exportDashboard=async(from:string,to:string)=>(await http.get<Blob>('/reports/dashboard.csv',{params:{from,to},responseType:'blob'})).data
export interface DocumentItem{id:string;title:string;description?:string;departmentId?:string;accessScope:string;sensitivity:string;status:string;currentVersion:number;filename:string;contentType:string;sizeBytes:number;updatedAt:string}
export const getDocuments=async(page=0,size=20)=>(await http.get<PageResponse<DocumentItem>>('/documents',{params:{page,size}})).data
export async function uploadDocument(metadata:{title:string;description?:string;departmentId?:string;accessScope:string;sensitivity:string},file:File){const body=new FormData();body.append('metadata',new Blob([JSON.stringify(metadata)],{type:'application/json'}));body.append('file',file);return(await http.post<DocumentItem>('/documents',body)).data}
export async function addDocumentVersion(id:string,file:File){const body=new FormData();body.append('file',file);return(await http.post<DocumentItem>(`/documents/${id}/versions`,body)).data}
export async function archiveDocument(id:string){await http.delete(`/documents/${id}`)}
export interface NotificationItem{id:string;type:string;title:string;message:string;resourceType?:string;resourceId?:string;readAt?:string;createdAt:string}
export const getNotifications=async()=>(await http.get<NotificationItem[]>('/notifications')).data
export const readNotification=async(id:string)=>http.post(`/notifications/${id}/read`)
export interface SecurityEvent{id:string;userId?:string;eventType:string;module:string;severity:string;occurredAt:string;indicators:string;context:string}
export interface SecurityProfile{userId:string;username:string;employeeId?:string;departmentId?:string;actions30d:number;denied30d:number;events30d:number;lastActivityAt?:string;lastSecurityEventAt?:string}
export interface SecurityProfileDetail{profile:SecurityProfile;currentRisk?:number;normalBehavior:string;recentBehavior:string;riskHistory:Array<{assessedAt:string;riskScore:number;severity:string;baselineScope:string;baselineSampleSize:number}>;alertHistory:Array<{id:string;status:string;severity:string;riskScore:number;createdAt:string}>;activityTimeline:Array<{id:string;occurredAt:string;action:string;module:string;result:string}>}
export interface SecurityAlert{id:string;userId:string;username:string;status:string;severity:string;riskScore:number;title:string;reasons:string;reviewedBy?:string;reviewedAt?:string;resolutionNote?:string;createdAt:string}
export interface RiskAssessment{id:string;userId:string;featureSnapshotId:string;modelVersion:string;anomalyScore:number;anomaly:boolean;riskScore:number;severity:string;contributions:Record<string,number>;baselineScope:'NONE'|'INDIVIDUAL'|'ROLE'|'DEPARTMENT'|'GLOBAL';baselineSampleSize:number;baseline:Record<string,number>;alertId?:string}
export interface SecurityOverview{alertsToday:number;criticalOpen:number;highRiskUsers:number;deniedAccess:number;alertTrend:Array<{date:string;value:number}>;riskTrend:Array<{date:string;value:number}>;alertsByModule:Array<{module:string;count:number}>}
export const getSecurityEvents=async(page=0,size=20)=>(await http.get<PageResponse<SecurityEvent>>('/security/events',{params:{page,size}})).data
export const getSecurityProfiles=async(page=0,size=20)=>(await http.get<PageResponse<SecurityProfile>>('/security/profiles',{params:{page,size}})).data
export const getSecurityProfile=async(id:string)=>(await http.get<SecurityProfileDetail>(`/security/profiles/${id}`)).data
export const getSecurityAlerts=async(status='',page=0,size=20,sort='createdAt')=>(await http.get<PageResponse<SecurityAlert>>('/security/alerts',{params:{status,page,size,sort}})).data
export const updateSecurityAlert=async(id:string,status:string,note:string)=>(await http.patch<SecurityAlert>(`/security/alerts/${id}`,{status,note})).data
export const analyzeUser=async(input:{userId:string;windowStart:string;windowEnd:string})=>(await http.post<RiskAssessment>('/security/analyze',input)).data
export const getSecurityOverview=async(days=30)=>(await http.get<SecurityOverview>('/security/overview',{params:{days}})).data
export interface CopilotAnswer{answer:string;provider:string;model:string;promptVersion:string;evidence:Array<{id:string;activityId:string;occurredAt:string;action:string;module:string;result:string}>}
export const askCopilot=async(alertId:string,question:string)=>(await http.post<CopilotAnswer>(`/security/alerts/${alertId}/copilot/ask`,{question})).data
