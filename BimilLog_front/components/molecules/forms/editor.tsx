"use client";



import React, { useEffect, useRef, useState } from "react";

import dynamic from "next/dynamic";

import { Spinner } from "@/components";

import { logger } from "@/lib/utils";



interface EditorProps {

  value: string;

  onChange: (value: string) => void;

  placeholder?: string;

}



/**

 * Quill ?먮뵒??而댄룷?뚰듃 - 寃뚯떆湲 ?묒꽦 ???ъ슜?섎뒗 由ъ튂 ?띿뒪???먮뵒?? * SSR ?댁뒋 諛⑹?瑜??꾪빐 dynamic import ?ъ슜

 * Quill 2.0 ?명솚??諛??덉젙?깆쓣 ?꾪븳 蹂듯빀??珥덇린??濡쒖쭅 ?ы븿

 */

const QuillEditor: React.FC<EditorProps> = ({

  value,

  onChange,

  placeholder = "내용을 입력하세요",

}) => {

  // DOM ?붿냼 諛?Quill ?몄뒪?댁뒪 李몄“

  const editorRef = useRef<HTMLDivElement>(null);

  const quillRef = useRef<unknown>(null);



  // 以묐났 珥덇린??諛⑹?瑜??꾪븳 ?뚮옒洹?
  const isInitializing = useRef(false);



  // 珥덇린 value ?ㅼ젙 ?щ? 異붿쟻

  const isInitialValueSet = useRef(false);



  // onChange瑜?ref濡?愿由ы븯???대줈? 臾몄젣 諛⑹?

  const onChangeRef = useRef(onChange);

  const placeholderRef = useRef(placeholder);

  const initialValueRef = useRef(value);



  // ?먮뵒???곹깭 愿由?
  const [isReady, setIsReady] = useState(false);

  const [error, setError] = useState<string | null>(null);



  // onChange媛 蹂寃쎈맆 ?뚮쭏??ref ?낅뜲?댄듃

  useEffect(() => {

    onChangeRef.current = onChange;

  }, [onChange]);



  useEffect(() => {

    placeholderRef.current = placeholder;

  }, [placeholder]);



  useEffect(() => {

    initialValueRef.current = value;

  }, [value]);



  useEffect(() => {

    /**

     * Quill ?먮뵒??珥덇린???⑥닔

     * 蹂듭옟??珥덇린??怨쇱젙???꾩슂???댁쑀:

     * 1. SSR ?섍꼍?먯꽌 window 媛앹껜 ?묎렐 諛⑹?

     * 2. 以묐났 珥덇린??諛⑹?

     * 3. Quill 2.0 踰꾩쟾??CSS ?숈쟻 濡쒕뵫

     * 4. 釉뚮씪?곗? ?명솚??臾몄젣 ?닿껐

     */

    const initQuill = async () => {

      // 珥덇린??議곌굔 泥댄겕: ?쒕쾭?ъ씠??DOM 誘몄?鍮?以묐났 珥덇린??諛⑹?

      if (

        typeof window === "undefined" ||

        !editorRef.current ||

        quillRef.current ||

        isInitializing.current

      ) {

        return;

      }



      try {

        isInitializing.current = true;

        logger.log("Quill ?먮뵒??珥덇린?붾? ?쒖옉?⑸땲??..");



        // Quill ?쇱씠釉뚮윭由щ? ?숈쟻?쇰줈 import (踰덈뱾 ?ш린 理쒖쟻??

        const { default: Quill } = await import("quill");



        /**

         * CSS ?숈쟻 濡쒕뵫 ?⑥닔

         * Quill 2.0? CSS媛 蹂꾨룄 濡쒕뵫?섏뼱???섎?濡??섎룞 濡쒕뵫

         * 以묐났 濡쒕뵫 諛⑹? 諛??먮윭 ?몃뱾留??ы븿

         */

        const loadCSS = (href: string, id: string) => {

          return new Promise<void>((resolve, reject) => {

            if (document.querySelector(`#${id}`)) {

              resolve();

              return;

            }



            const link = document.createElement("link");

            link.id = id;

            link.rel = "stylesheet";

            link.href = href;

            link.onload = () => resolve();

            link.onerror = () =>

              reject(new Error(`Failed to load CSS: ${href}`));

            document.head.appendChild(link);

          });

        };



        // Quill CSS ?뚯씪?ㅼ쓣 蹂묐젹濡?濡쒕뱶

        await Promise.all([

          loadCSS(

            "https://cdn.jsdelivr.net/npm/quill@2.0.3/dist/quill.core.css",

            "quill-core-css"

          ),

          loadCSS(

            "https://cdn.jsdelivr.net/npm/quill@2.0.3/dist/quill.snow.css",

            "quill-snow-css"

          ),

        ]);



        // ?먮뵒???믪씠 怨좎젙???꾪븳 而ㅼ뒪? CSS 異붽?

        const styleId = "quill-editor-height-fix";

        if (!document.querySelector(`#${styleId}`)) {

          const style = document.createElement("style");

          style.id = styleId;

          style.textContent = `

            .ql-container {

              font-size: 14px;

            }

            .ql-editor {

              max-height: 300px;

              overflow-y: auto;

              min-height: 200px;

            }

            .ql-editor::-webkit-scrollbar {

              width: 8px;

            }

            .ql-editor::-webkit-scrollbar-track {

              background: #f1f1f1;

              border-radius: 4px;

            }

            .ql-editor::-webkit-scrollbar-thumb {

              background: #888;

              border-radius: 4px;

            }

            .ql-editor::-webkit-scrollbar-thumb:hover {

              background: #555;

            }

          `;

          document.head.appendChild(style);

        }



        // CSS ?ㅽ????곸슜 ?湲?(?뚮뜑留??꾨즺 蹂댁옣)

        await new Promise((resolve) => setTimeout(resolve, 300));



        // 湲곗〈 ?대컮媛 ?덉쑝硫??쒓굅 (以묐났 諛⑹?)

        const existingToolbar = editorRef.current?.querySelector('.ql-toolbar');

        if (existingToolbar) {

          logger.log("湲곗〈 ?대컮 ?쒓굅 以?..");

          existingToolbar.remove();

        }



        /**

         * Quill ?몄뒪?댁뒪 ?앹꽦

         * toolbar? formats瑜?Quill 2.0??留욊쾶 ?덉쟾?섍쾶 ?ㅼ젙

         * 而ㅻ??덊떚 寃뚯떆湲 ?묒꽦???꾩슂??湲곕낯?곸씤 ?쒖떇留??ы븿

         */

        quillRef.current = new Quill(editorRef.current, {

          theme: "snow",

          placeholder: placeholderRef.current,

          modules: {

            toolbar: [

              [{ header: [1, 2, false] }],

              ["bold", "italic", "underline", "strike"],

              [{ color: [] }, { background: [] }],

              [{ list: "ordered" }, { list: "bullet" }],

              [{ align: [] }],

              ["blockquote", "code-block"],

              ["link"],

              ["clean"],

            ],

          },

          // XSS 諛⑹?瑜??꾪빐 ?덉쟾???щ㎎?ㅻ쭔 ?덉슜

          formats: [

            "header",

            "bold",

            "italic",

            "underline",

            "strike",

            "color",

            "background",

            "list",

            "align",

            "blockquote",

            "code-block",

            "link",

          ],

        });



        const quill = quillRef.current as {

          on: (event: string, handler: () => void) => void;

          getSemanticHTML?: () => string;

          root: { innerHTML: string };

          clipboard: { convert: (options: { html: string }) => unknown };

          setContents: (delta: unknown, source: string) => void;

          off?: (event: string, handler?: () => void) => void;

        };



        /**

         * ?띿뒪??蹂寃??대깽??由ъ뒪???ㅼ젙

         * getSemanticHTML() 硫붿꽌???ъ슜???곗꽑?섎릺,

         * ?놁쓣 寃쎌슦 innerHTML濡??대갚 (Quill 踰꾩쟾 ?명솚??

         */

        quill.on("text-change", () => {

          try {

            const content = quill.getSemanticHTML

              ? quill.getSemanticHTML()

              : quill.root.innerHTML;

            onChangeRef.current(content);

            // ?ъ슜?먭? ??댄븨???쒖옉?섎㈃ 珥덇린 value ?ㅼ젙 ?꾨즺濡??쒖떆 (?댄썑 ?몃? ?숆린??諛⑹?)

            isInitialValueSet.current = true;

          } catch (err) {

            logger.error("Error getting content:", err);

            onChangeRef.current(quill.root.innerHTML);

            isInitialValueSet.current = true;

          }

        });



        // 湲곗〈 ?댁슜???덈뒗 寃쎌슦 ?먮뵒?곗뿉 ?ㅼ젙 (珥덇린??????踰덈쭔)

        const initialValue = initialValueRef.current;

        if (initialValue) {

          try {

            const delta = quill.clipboard.convert({ html: initialValue });

            quill.setContents(delta, "silent");

            // 초기 value 적용 완료 표시

            isInitialValueSet.current = true;

          } catch (err) {

            logger.error("Error setting initial content:", err);

            quill.root.innerHTML = initialValue;

            // 초기 value 적용 완료 표시

            isInitialValueSet.current = true;

          }

        }



        // value媛 鍮꾩뼱?덉쑝硫?isInitialValueSet??false濡??좎??섏뿬 ??踰덉㎏ useEffect?먯꽌 泥섎━



        /**

         * SVG ?꾩씠肄??뚮뜑留?臾몄젣 ?닿껐

         * Quill??SVG ?꾩씠肄섏쓣 ?띿뒪?몃줈 ?섎せ ?뚮뜑留곹븯??寃쎌슦媛 ?덉뼱

         * ?대컮 踰꾪듉?먯꽌 ?섎せ???띿뒪???몃뱶瑜??쒓굅

         */

        setTimeout(() => {

          const toolbar = editorRef.current?.querySelector(".ql-toolbar");

          if (toolbar) {

            const buttons = toolbar.querySelectorAll("button");

            buttons.forEach((button) => {

              const textNodes = Array.from(button.childNodes).filter(

                (node) =>

                  node.nodeType === Node.TEXT_NODE &&

                  node.textContent?.includes("viewBox")

              );

              textNodes.forEach((node) => node.remove());

            });

          }

        }, 100);



        setIsReady(true);

        setError(null);

        logger.log("Quill ?먮뵒?곌? ?깃났?곸쑝濡?珥덇린?붾릺?덉뒿?덈떎.");

      } catch (error) {

        logger.error("Quill 濡쒕뱶 ?ㅽ뙣:", error);

        setError(

          error instanceof Error ? error.message : "?먮뵒??濡쒕뱶???ㅽ뙣?덉뒿?덈떎."

        );

        setIsReady(true);

      } finally {

        isInitializing.current = false;

      }

    };



    initQuill();



    const quillInstance = quillRef.current;

    const editorElement = editorRef.current;



    // 而댄룷?뚰듃 ?몃쭏?댄듃 ??硫붾え由??꾩닔 諛⑹?瑜??꾪븳 ?뺣━

    return () => {

      if (quillInstance) {

        try {

          (quillInstance as { off: (event: string) => void }).off("text-change");

          // DOM ?뺣━ - ?대컮 ?쒓굅

          const toolbar = editorElement?.querySelector('.ql-toolbar');

          toolbar?.remove();

        } catch (err) {

          logger.error("Error cleaning up Quill:", err);

        }

      }

    };

    // 珥덇린 留덉슫???쒖뿉留?Quill??珥덇린?뷀븯怨? ?댄썑?먮뒗 ?ъ큹湲고솕?섏? ?딆쓬

  }, []);



  /**

   * ?몃??먯꽌 value prop??蹂寃쎈릺?덉쓣 ???먮뵒???댁슜 ?숆린??   * 珥덇린 value ?ㅼ젙 ?댄썑?먮뒗 ?ъ슜???낅젰留?諛섏쁺?섍린 ?꾪빐 ?숆린?뷀븯吏 ?딆쓬

   * ?꾩떆???蹂듭썝 ???몃? 蹂寃쎌? 而댄룷?뚰듃 ?щ쭏?댄듃濡?泥섎━

   */

  useEffect(() => {

    // 珥덇린 value ?ㅼ젙???꾨즺??寃쎌슦?먮뒗 ?숆린?뷀븯吏 ?딆쓬 (?ъ슜???낅젰 ?곗꽑)

    if (!quillRef.current || !isReady || error || isInitialValueSet.current) {

      return;

    }



    // 珥덇린 value媛 ?덇퀬 ?꾩쭅 ?ㅼ젙?섏? ?딆? 寃쎌슦?먮쭔 ?숆린??(?꾩떆???蹂듭썝 ??

    if (value) {

      try {

        const quill = quillRef.current as {

          clipboard: { convert: (options: { html: string }) => unknown };

          setContents: (delta: unknown, source: string) => void;

        };

        const delta = quill.clipboard.convert({ html: value });

        quill.setContents(delta, "silent");

        isInitialValueSet.current = true;

        logger.log("?몃??먯꽌 value媛 蹂寃쎈릺???먮뵒???댁슜???숆린?뷀뻽?듬땲??");

      } catch (err) {

        logger.error("Error updating content:", err);

      }

    }

  }, [value, isReady, error]);



  /**

   * ?먮윭 諛쒖깮 ???대갚 ?먮뵒???뚮뜑留?   * Quill 濡쒕뱶 ?ㅽ뙣 ?쒖뿉??湲곕낯 ?띿뒪???낅젰??媛?ν븯?꾨줉 ??   * HTML ?쒓렇???쒓굅?섏뿬 ?뚮젅???띿뒪?몃줈 ?몄쭛

   */

  if (error) {

    return (

      <div className="w-full">

        <div className="h-[400px] border border-gray-200 rounded-lg bg-white flex flex-col">

          <div className="p-3 bg-gray-50 border-b rounded-t-lg">

            <p className="text-sm text-brand-muted">

              간단 작성기 (에디터 로드 실패)

            </p>

          </div>

          <textarea

            className="w-full flex-1 p-4 border-0 resize-none focus:outline-none"

            placeholder={placeholder}

            value={value.replace(/<[^>]*>/g, "")} // HTML ?쒓렇 ?쒓굅

            onChange={(e) => onChange(e.target.value)}

          />

        </div>

        <p className="text-xs text-red-500 mt-1">

          고급 작성기를 로드할 수 없어 간단 작성기로 전환하였습니다

        </p>

      </div>

    );

  }



  return (

    <div className="w-full relative">

      {/* Quill ?먮뵒?곌? 留덉슫?몃맆 DOM ?붿냼 */}

      <div

        ref={editorRef}

        className="bg-white h-[400px] rounded-lg border border-gray-200"

        style={{

          fontSize: "14px",

          lineHeight: "1.5",

          display: "flex",

          flexDirection: "column",

        }}

      />

      {/* ?먮뵒??珥덇린??以?濡쒕뵫 ?ㅻ쾭?덉씠 */}

      {!isReady && (

        <div className="absolute inset-0 bg-white bg-opacity-90 flex items-center justify-center rounded-lg z-10">

          <div className="flex flex-col items-center gap-2">

            <Spinner size="md" />

            <p className="text-sm text-brand-secondary">에디터 준비 중..</p>

          </div>

        </div>

      )}

    </div>

  );

};



/**

 * ?먮뵒??濡쒕뵫 以??쒖떆?섎뒗 而댄룷?뚰듃

 * dynamic import ?湲??쒓컙 ?숈븞 ?ъ슜?먯뿉寃?濡쒕뵫 ?곹깭瑜?蹂댁뿬以? */

const EditorLoading = () => (

  <div className="relative h-[400px] bg-white rounded-lg border border-gray-200 flex items-center justify-center">

    <div className="flex flex-col items-center gap-2">

      <Spinner size="md" />

      <p className="text-sm text-brand-secondary">에디터 로딩 중..</p>

    </div>

  </div>

);



/**

 * 硫붿씤 ?먮뵒??而댄룷?뚰듃 (Dynamic Import)

 * SSR ?섍꼍?먯꽌 window 媛앹껜 ?묎렐 臾몄젣瑜?諛⑹??섍린 ?꾪빐

 * ?대씪?댁뼵???ъ씠?쒖뿉?쒕쭔 濡쒕뱶?섎룄濡??ㅼ젙

 */

const Editor = dynamic(() => Promise.resolve(QuillEditor), {

  ssr: false,

  loading: () => <EditorLoading />,

});



export default Editor;



