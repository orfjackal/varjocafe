(ns varjocafe.format-test
  (:use midje.sweet)
  (:require [varjocafe.format :as format]
            [net.cgrand.enlive-html :as html]))

(defn render [nodes]
  (apply str (html/emit* nodes)))

(fact "Allergens format"
      (fact "No allergens"
            (format/allergens {:name "Spam"
                               :meta {:0 [],
                                      :1 [],
                                      :2 []}})
            => "")
      (fact "Has allergens"
            (format/allergens {:name "Spam"
                               :meta {:0 ["PÄ" "V"],
                                      :1 [],
                                      :2 []}})
            => "PÄ, V")
      (fact "Other ingredient warnings"
            (format/allergens {:name "Spam"
                               :meta {:0 ["PÄ" "V"],
                                      :1 ["valkosipulia"],
                                      :2 []}})
            => "PÄ, V, valkosipulia")
      (fact "Additional information"
            (format/allergens {:name "Spam"
                               :meta {:0 ["PÄ" "V"],
                                      :1 ["valkosipulia"],
                                      :2 ["Ilmastovalinta"]}})
            => "PÄ, V, valkosipulia, Ilmastovalinta"))

(fact "Day range format"
      (fact "Contiguous full week"
            (format/day-range ["Ma" "Ti" "Ke" "To" "Pe" "La" "Su"]) => "Ma-Su")
      (fact "Contiguous beginning of week"
            (format/day-range ["Ma" "Ti" "Ke" "To" "Pe" "La" false]) => "Ma-La")
      (fact "Contiguous end of week"
            (format/day-range [false "Ti" "Ke" "To" "Pe" "La" "Su"]) => "Ti-Su")
      (fact "Contiguous minimum range"
            (format/day-range ["Ma" "Ti" false false false false false]) => "Ma-Ti")
      (fact "Multiple contiguous ranges"
            (format/day-range ["Ma" "Ti" false "To" "Pe" false false]) => "Ma-Ti, To-Pe")
      (fact "Non-contiguous"
            (format/day-range ["Ma" false "Ke" false "Pe" false "Su"]) => "Ma, Ke, Pe, Su")
      (fact "Single day"
            (format/day-range ["Ma" false false false false false false]) => "Ma")
      (fact "No days"
            (format/day-range [false false false false false false false]) => ""))

(fact "Opening times format"
      (fact "Contiguous date ranges are delimited with a dash"
            (format/opening-times [{:when ["Ma" "Ti" "Ke" "To" "Pe" false false]
                                    :open "10:30", :close "16:00"}])
            => ["Ma-Pe" "10:30-16:00"])
      (fact "Non-contiguous date ranges are delimited with a comma"
            (format/opening-times [{:when ["Ma" false "Ke" false "Pe" false false]
                                    :open "10:30", :close "16:00"}])
            => ["Ma, Ke, Pe" "10:30-16:00"])
      (fact "Opening and closing times may vary by day of week"
            (format/opening-times [{:when ["Ma" "Ti" "Ke" "To" false false false]
                                    :open "10:30", :close "16:00"}
                                   {:when [false false false false "Pe" false false]
                                    :open "10:30", :close "15:00"}])
            => ["Ma-To" "10:30-16:00"
                "Pe" "10:30-15:00"])
      (fact "Not open"
            (format/opening-times [{:when [false false false false false false false]
                                    :open "", :close ""}])
            => []
            (format/opening-times [{:when ["Ma" "Ti" "Ke" "To" "Pe" false false]
                                    :open "", :close ""}])
            => [])
      (fact "HTML format"
            (render (format/opening-times-html [{:when ["Ma" false false false false false false]
                                                 :open "10:30", :close "16:00"}
                                                {:when [false "Ti" "Ke" "To" false false false]
                                                 :open "10:30", :close "15:00"}
                                                {:when [false false false false "Pe" false false]
                                                 :open "10:30", :close "14:00"}]))
            => (str "<span class=\"dates\">Ma</span> <span class=\"times\">10:30-16:00</span><br />"
                    "<span class=\"dates\">Ti-To</span> <span class=\"times\">10:30-15:00</span><br />"
                    "<span class=\"dates\">Pe</span> <span class=\"times\">10:30-14:00</span>"))
      (fact "No HTML when not open"
            (format/opening-times-html [{:when [false false false false false false false]
                                         :open "", :close ""}])
            => nil))

(fact "#opening-times-title"
      (fact "Default name"
            (format/opening-times-title {} :bistro) => "Bistro")
      (fact "Custom name"
            (format/opening-times-title {:information {:bistro {:name "Pizza"}}}
                                        :bistro) => "Pizza")
      (fact "Unknown category"
            (format/opening-times-title {} :foo) => "???"))

(fact "Exception format"
      (fact "Closed"
            (format/exception {} {:category :business
                                               :closed   true}) => "Ravintola suljettu")
      (fact "No lunch"
            (format/exception {} {:category :lounas
                                               :closed   true}) => "Ei lounasta")
      (fact "Bistro closed"
            (format/exception {} {:category :bistro
                                               :closed   true}) => "Bistro suljettu")

      (fact "Custom opening hours"
            (format/exception {} {:category :business
                                               :open     "08:00"
                                               :close    "14:00"}) => "Auki 08:00-14:00")
      (fact "Custom lunch hours"
            (format/exception {} {:category :lounas
                                               :open     "10:00"
                                               :close    "14:00"}) => "Lounas 10:00-14:00"))
