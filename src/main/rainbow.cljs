(ns rainbow
  (:require
   ["/rainbowkit" :as rainbowkit-without]
   ["@rainbow-me/rainbowkit" :as rainbowkit]
   ["wagmi" :as wagmi]
   ["wagmi/chains" :as wagmi.chains]
   ["wagmi/providers/public" :as public]
   [reagent.dom :as rdom]))

(def config (wagmi/configureChains #js[wagmi.chains/polygon]
                                   #js[(public/publicProvider)]))

(def wallets-without (rainbowkit-without/getDefaultWallets #js{:appName "rainbow-without"

                                                               :chains (.-chains config)}))

(def client-without (wagmi/createClient #js{:autoConnect true
                                            :connectors (.-connectors wallets-without)
                                            :provider (.-provider config)}))

(defn provider-without []
  [:> wagmi/WagmiConfig {:client client-without}
   [:> rainbowkit-without/RainbowKitProvider {:chains (.-chains config)
                                              :theme (rainbowkit-without/darkTheme)
                                              :id "rainbow_without_root"}
    [:div
     [:> rainbowkit-without/ConnectButton]]]])

(def wallets (rainbowkit/getDefaultWallets #js{:appName "rainbow"
                                               :chains (.-chains config)}))

(def client (wagmi/createClient #js{:autoConnect true
                                    :connectors (.-connectors wallets)
                                    :provider (.-provider config)}))

(defn provider []
  [:> wagmi/WagmiConfig {:client client}
   [:> rainbowkit/RainbowKitProvider {:chains (.-chains config)
                                      :theme (rainbowkit/darkTheme)
                                      :id "rainbow_root"}
    [:div
     [:> rainbowkit/ConnectButton]]]])

(defn view []
  [:div
   [:div "With dynamic imports"]
   [provider]
   [:div "Without dynamic imports"]
   [provider-without]])

(defn ^:export init
  []
  (rdom/render [view] (js/document.getElementById "root")))

(defn ^:dev/after-load reload []
  (rdom/render [view] (js/document.getElementById "root")))
