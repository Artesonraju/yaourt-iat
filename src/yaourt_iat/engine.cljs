(ns yaourt-iat.engine)

(defn fixed-left-right
  [length factor]
  (repeat length {:left [(factor :left)] :right [(factor :right)]}))

(defn random-left-right
  [length categories]
  (let [items [{:left [(first categories)] :right [(second categories)]}
               {:right [(first categories)] :left [(second categories)]}]]
    (if (even? length)
      (shuffle (mapcat #(repeat (/ length 2) %) items))
      (rest (shuffle (mapcat #(repeat (+ (/ length 2) 1) %) items))))))

(defn left-right
  [factor]
  (case (factor :random)
    true (let [categories (keys (factor :categories))
            f (first categories)
            s (second categories)]
            [{:left [f] :right [s]}
             {:right [f] :left [s]}])
    [{:left [(factor :left)] :right [(factor :right)]}]))

(defn cartesian-merge [colls]
  (if (empty? colls)
    '({})
    (for [x (first colls)
          more (cartesian-merge (rest colls))]
      (merge-with concat x more))))

(defn build-left-rights
  [factors block]
  (cartesian-merge (map #(left-right (factors %)) (block :factors))))

(defn assoc-left-right
  [fs block steps]
  (let [lrs (build-left-rights fs block)]
    (for [step steps lf lrs]
      (merge step lf))))

(defn build-category
  [fname factor colors name category]
  (apply concat (for [[type set] category]
    (map #(identity {
                     :colors colors
                     :target {:type type :value %}
                     :color (factor :color)
                     :category name
                     :factor fname}) set))))

(defn build-words [factors name]
  (let [factor (factors name)
        colors (map #(% :color) (vals factors))
        categories (keys (factor :categories))]
    (apply concat (for [[k c] (factor :categories)]
        (build-category name factor colors k c)))))

(defn assoc-expected [step]
  (assoc step :expected
    (if (some #(= (step :category) %) (step :right))
      :right
      :left)))

(defn build-factor
  [factors name block]
  (->> (build-words factors name)
       (assoc-left-right factors block)
       (map assoc-expected)))

(defn add-ids
  [coll]
  (map-indexed (fn [id v] (assoc v :id id)) coll))

(defn build-block
  [factors block]
  (let [block-factors (block :factors)
        fs (select-keys factors block-factors)
        pre-steps (apply concat (for [[n _] fs] (build-factor fs n block)))
        steps (reduce into (repeatedly (:times block) #(shuffle pre-steps)))
        random-step (rand-nth steps)]
    (into [(merge random-step {:type :page/instruction :text (block :instruction)})]
                      (mapcat (juxt #(assoc % :type :page/label)
                                    #(assoc % :type :page/cross)
                                    #(assoc % :type :page/target)
                                    #(assoc % :type :page/wrong)
                                    #(assoc % :type :page/transition))
                                    steps))))

(defn fix-factors
  [factors]
  (into {} (for [[k v] factors]
             (case (v :random)
               true [k v]
               false [k (merge v (zipmap [:left :right] (keys (v :categories))))]
               [k (merge v (zipmap (shuffle [:left :right]) (keys (v :categories))))]))))

(defn all-images
  [factors]
  (->> (vals factors)
       (mapcat #(vals (get % :categories)))
       (mapcat #(get % :target/image))
       (map #(identity {:type :target/image :value %}))))

(defn init-data
  [conf]
  (let [factors (fix-factors (conf :factors))
        images (all-images factors)
        steps (mapcat (partial build-block factors) (conf :blocks))
        intro {:type :page/intro :text (conf :intro) :img-count 0 :error false :loader images}
        end {:type :page/end :text (conf :end)}
        pages (into [] (add-ids (concat [intro] steps [end])))]
    identity {:page/pages pages
              :page/current 0}))
