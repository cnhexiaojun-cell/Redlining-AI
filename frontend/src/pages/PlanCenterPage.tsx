import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { QRCodeSVG } from "qrcode.react";
import { useAuth } from "../contexts/AuthContext";
import { useLanguage } from "../contexts/LanguageContext";
import { getPlans, createOrder, completeOrder, getOrder, type PlanDto, type OrderDto } from "../api/plans";
import Modal from "../components/Modal";

type TabFilter = "all" | "quota" | "subscription";

export default function PlanCenterPage() {
  const { t } = useLanguage();
  const { token, user, refreshUser } = useAuth();
  const [plans, setPlans] = useState<PlanDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<TabFilter>("all");
  const [purchasePlan, setPurchasePlan] = useState<PlanDto | null>(null);
  const [paymentMethod, setPaymentMethod] = useState<"wechat" | "alipay">("wechat");
  const [purchasing, setPurchasing] = useState(false);
  const [purchaseError, setPurchaseError] = useState("");
  const [purchaseSuccess, setPurchaseSuccess] = useState(false);
  const [pendingOrder, setPendingOrder] = useState<OrderDto | null>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const load = useCallback(() => {
    if (!token) return;
    setLoading(true);
    getPlans(token)
      .then(setPlans)
      .catch(() => setPlans([]))
      .finally(() => setLoading(false));
  }, [token]);

  useEffect(() => {
    load();
  }, [load]);

  const filtered = tab === "all" ? plans : plans.filter((p) => p.type === tab);
  const isSubscription = user?.planType === "subscription";
  const canRenew = isSubscription && user?.periodEndsAt;

  const handleBuyClick = (plan: PlanDto) => {
    setPurchasePlan(plan);
    setPurchaseError("");
    setPurchaseSuccess(false);
    setPendingOrder(null);
  };

  const handlePurchaseConfirm = async () => {
    if (!token || !purchasePlan) return;
    setPurchasing(true);
    setPurchaseError("");
    setPurchaseSuccess(false);
    try {
      const isRenewal = canRenew && user?.planCode === purchasePlan.code;
      const order = await createOrder(token, {
        planId: purchasePlan.id,
        paymentMethod,
        renewal: Boolean(isRenewal),
      });
      if (paymentMethod === "wechat" && order.codeUrl) {
        setPendingOrder(order);
        setPurchasing(false);
        return;
      }
      if (paymentMethod === "wechat" && !order.codeUrl) {
        setPurchaseError(t("plan_wechat_not_configured") + " " + t("plan_wechat_config_hint"));
        setPurchaseSuccess(false);
        setPurchasing(false);
        return;
      }
      if (paymentMethod === "alipay") {
        setPurchaseError(t("plan_alipay_coming"));
        setPurchaseSuccess(false);
        setPurchasing(false);
        return;
      }
      setPurchasing(false);
    } catch (e) {
      setPurchaseError((e as Error).message);
      setPurchaseSuccess(false);
      setPurchasing(false);
    }
  };

  useEffect(() => {
    if (!pendingOrder || !token) return;
    const POLL_INTERVAL_MS = 2000;
    const TIMEOUT_MS = 5 * 60 * 1000;
    const startedAt = Date.now();
    pollRef.current = setInterval(async () => {
      if (Date.now() - startedAt > TIMEOUT_MS) {
        if (pollRef.current) clearInterval(pollRef.current);
        pollRef.current = null;
        setPendingOrder(null);
        return;
      }
      try {
        const order = await getOrder(token, pendingOrder.id);
        if (order.status === "paid") {
          if (pollRef.current) clearInterval(pollRef.current);
          pollRef.current = null;
          setPendingOrder(null);
          setPurchaseSuccess(true);
          if (refreshUser) await refreshUser();
          load();
          setTimeout(() => {
            setPurchasePlan(null);
            setPurchaseSuccess(false);
          }, 1500);
        }
      } catch {
        // ignore poll errors
      }
    }, POLL_INTERVAL_MS);
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
      pollRef.current = null;
    };
  }, [pendingOrder?.id, token, refreshUser, load]);

  return (
    <div className="w-full max-w-5xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">{t("plan_center")}</h1>
      <p className="text-gray-600 mb-6">{t("plan_center_desc")}</p>

      {user?.planName != null && (
        <div className="mb-6 p-4 bg-blue-50 border border-blue-100 rounded-xl text-sm text-gray-700">
          {t("plan_current")}: <strong>{user.planName}</strong>
          {user.quotaTotal != null && user.quotaTotal > 0 && (
            <> · {t("plan_quota_remaining").replace("{remaining}", String(user.quotaRemaining ?? 0)).replace("{total}", String(user.quotaTotal))}</>
          )}
          {user.quotaTotal != null && user.quotaTotal <= 0 && <> · {t("plan_quota_unlimited")}</>}
          {user.periodEndsAt != null && <> · {t("plan_period_ends")} {new Date(user.periodEndsAt).toLocaleDateString()}</>}
        </div>
      )}

      <div className="flex gap-2 mb-6 border-b border-gray-200">
        <button
          type="button"
          onClick={() => setTab("all")}
          className={`px-4 py-2 text-sm font-medium rounded-t-lg ${tab === "all" ? "bg-gray-100 text-gray-900 border border-b-0 border-gray-200" : "text-gray-600 hover:bg-gray-50"}`}
        >
          {t("plan_tab_all")}
        </button>
        <button
          type="button"
          onClick={() => setTab("quota")}
          className={`px-4 py-2 text-sm font-medium rounded-t-lg ${tab === "quota" ? "bg-gray-100 text-gray-900 border border-b-0 border-gray-200" : "text-gray-600 hover:bg-gray-50"}`}
        >
          {t("admin_plans_type_quota")}
        </button>
        <button
          type="button"
          onClick={() => setTab("subscription")}
          className={`px-4 py-2 text-sm font-medium rounded-t-lg ${tab === "subscription" ? "bg-gray-100 text-gray-900 border border-b-0 border-gray-200" : "text-gray-600 hover:bg-gray-50"}`}
        >
          {t("admin_plans_type_subscription")}
        </button>
      </div>

      {tab === "subscription" && (
        <p className="mb-4 text-sm text-gray-500">{t("plan_subscription_hint")}</p>
      )}

      {loading ? (
        <div className="text-gray-500 py-8">{t("admin_loading")}</div>
      ) : filtered.length === 0 ? (
        <div className="text-gray-500 py-8">{t("admin_none")}</div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((plan) => {
            const periodLabel = plan.period === "year" ? t("admin_plans_period_year") : t("admin_plans_period_month");
            const isUnlimitedQuota = plan.quota == null || plan.quota < 0;
            return (
            <div
              key={plan.id}
              className="border border-gray-200 rounded-xl p-5 bg-white shadow-sm hover:shadow-md transition-shadow relative"
            >
              {plan.type === "subscription" && (
                <span className="absolute top-3 right-3 text-xs font-medium px-2 py-0.5 rounded-full bg-blue-100 text-blue-700">
                  {t("plan_recommended")}
                </span>
              )}
              <div className="font-semibold text-gray-900">{plan.name}</div>
              <div className="mt-1 flex items-center gap-2">
                <span className="text-xs font-medium px-2 py-0.5 rounded bg-gray-100 text-gray-700">
                  {plan.type === "quota" ? t("admin_plans_type_quota") : t("admin_plans_type_subscription")}
                </span>
                {plan.type === "subscription" && plan.period && (
                  <span className="text-xs text-gray-500">{periodLabel}</span>
                )}
              </div>
              {/* 分析次数与支付说明 */}
              <ul className="mt-3 space-y-1.5 text-sm text-gray-600">
                {plan.type === "quota" ? (
                  <>
                    <li>
                      {isUnlimitedQuota
                        ? t("plan_detail_quota_unlimited")
                        : t("plan_detail_quota_includes").replace("{n}", String(plan.quota))}
                    </li>
                    <li className="text-gray-500">{t("plan_detail_quota_one_time")}</li>
                  </>
                ) : (
                  <>
                    <li>
                      {plan.period
                        ? (isUnlimitedQuota
                          ? t("plan_detail_subscription_unlimited").replace("{period}", periodLabel)
                          : t("plan_detail_subscription_per_period").replace("{n}", String(plan.quota)).replace("{period}", periodLabel))
                        : (isUnlimitedQuota ? t("plan_detail_quota_unlimited") : t("plan_detail_quota_includes").replace("{n}", String(plan.quota)))}
                    </li>
                    <li className="text-gray-500">{t("plan_detail_subscription_period_valid")}</li>
                  </>
                )}
              </ul>
              <div className="mt-3 text-lg font-medium text-gray-900">
                ¥{(plan.priceCents / 100).toFixed(2)}
                {plan.type === "subscription" && plan.period && (
                  <span className="text-sm font-normal text-gray-500">/{periodLabel}</span>
                )}
              </div>
              {plan.description != null && plan.description !== "" && (
                <p className="mt-2 text-sm text-gray-600">{plan.description}</p>
              )}
              {plan.scope != null && plan.scope !== "" && (
                <p className="mt-1 text-xs text-gray-500">{t("admin_plans_scope")}: {plan.scope}</p>
              )}
              {(plan.code !== "free" && plan.priceCents > 0) && (
                <div className="mt-4 flex gap-2">
                  {plan.type === "subscription" && canRenew && user?.planCode === plan.code ? (
                    <button type="button" onClick={() => handleBuyClick(plan)} className="px-3 py-1.5 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700">
                      {t("plan_renew")}
                    </button>
                  ) : (
                    <button type="button" onClick={() => handleBuyClick(plan)} className="px-3 py-1.5 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700">
                      {t("plan_buy")}
                    </button>
                  )}
                </div>
              )}
            </div>
          );
          })}
        </div>
      )}

      <Modal
        open={purchasePlan != null}
        title={purchasePlan ? (canRenew && user?.planCode === purchasePlan.code ? t("plan_renew") : t("plan_buy")) : ""}
        onClose={() => {
          setPurchasePlan(null);
          setPendingOrder(null);
          setPurchaseError("");
          setPurchaseSuccess(false);
        }}
      >
        {purchasePlan && (() => {
          if (pendingOrder?.codeUrl) {
            return (
              <div className="space-y-4">
                <p className="text-sm text-gray-600">{purchasePlan.name} · ¥{(pendingOrder.amountCents / 100).toFixed(2)}</p>
                <p className="text-sm font-medium text-gray-700">{t("plan_wechat_scan_hint")}</p>
                <div className="flex justify-center p-4 bg-white rounded-lg border border-gray-200">
                  <QRCodeSVG value={pendingOrder.codeUrl} size={220} level="M" />
                </div>
                <p className="text-sm text-gray-500">{t("plan_wechat_waiting")}</p>
                <div className="flex justify-end">
                  <button
                    type="button"
                    onClick={() => {
                      setPurchasePlan(null);
                      setPendingOrder(null);
                      setPurchaseError("");
                      setPurchaseSuccess(false);
                    }}
                    className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg"
                  >
                    {t("admin_cancel")}
                  </button>
                </div>
              </div>
            );
          }
          const periodLabel = purchasePlan.period === "year" ? t("admin_plans_period_year") : t("admin_plans_period_month");
          const isUnlimited = purchasePlan.quota == null || purchasePlan.quota < 0;
          const detailSummary =
            purchasePlan.type === "quota"
              ? isUnlimited
                ? t("plan_detail_quota_unlimited") + " · " + t("plan_detail_quota_one_time")
                : t("plan_detail_quota_includes").replace("{n}", String(purchasePlan.quota)) + " · " + t("plan_detail_quota_one_time")
              : purchasePlan.period
                ? (isUnlimited
                  ? t("plan_detail_subscription_unlimited").replace("{period}", periodLabel)
                  : t("plan_detail_subscription_per_period").replace("{n}", String(purchasePlan.quota)).replace("{period}", periodLabel)) + " · " + t("plan_detail_subscription_period_valid")
                : purchasePlan.name;
          return (
            <div className="space-y-4">
              <p className="font-medium text-gray-900">{purchasePlan.name}</p>
              <p className="text-sm text-gray-600">{detailSummary}</p>
              <p className="text-lg font-medium text-gray-900">¥{(purchasePlan.priceCents / 100).toFixed(2)}{purchasePlan.type === "subscription" && purchasePlan.period ? `/${periodLabel}` : ""}</p>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t("plan_payment_method")}</label>
                <select value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value as "wechat" | "alipay")} className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
                  <option value="wechat">{t("plan_pay_wechat")}</option>
                  <option value="alipay">{t("plan_pay_alipay")}</option>
                </select>
              </div>
              {purchaseError && <p className="text-sm text-red-600">{purchaseError}</p>}
              {purchaseSuccess && !purchaseError && <p className="text-sm text-green-600">{t("plan_purchase_success")}</p>}
              <div className="flex justify-end gap-2">
                <button type="button" onClick={() => { setPurchasePlan(null); setPendingOrder(null); setPurchaseError(""); setPurchaseSuccess(false); }} className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg">
                  {t("admin_cancel")}
                </button>
                <button type="button" onClick={handlePurchaseConfirm} disabled={purchasing} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50">
                  {purchasing ? t("admin_submitting") : t("plan_confirm_pay")}
                </button>
              </div>
            </div>
          );
        })()}
      </Modal>

      <p className="mt-8 text-center text-sm text-gray-500 flex flex-wrap justify-center gap-4">
        <Link to="/plans/orders" className="text-blue-600 hover:underline">{t("plan_order_history")}</Link>
        <Link to="/profile" className="text-blue-600 hover:underline">{t("nav_profile")}</Link>
      </p>
    </div>
  );
}
