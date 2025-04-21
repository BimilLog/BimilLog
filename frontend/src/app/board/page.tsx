import {SimplePostDTO} from "@/components/types/schema";


export default function BoardPage() {
    const simplePostDTO: SimplePostDTO[] = [
        {
            postId: 1,
            userId: 101,
            farmName: "행복농장",
            title: "이번 주 딸기 수확 후기",
            commentCount: 3,
            likes: 10,
            views: 123,
            is_featured: false,
            is_notice: false,
            createdAt: "2025-04-20T12:34:56Z",
        },
    ];
    return (
        <main className="flex-shrink-0">
            {/* 게시판 헤더 */}
            <header className="py-1 bg-white">
                <div className="text-center my-3"></div>
            </header>

            {/* 게시판 본문 */}
            <div className="container px-5">
                <div className="row">
                    {/* 게시글 목록 */}
                    <div className="col-lg-8">
                        {/* 게시판 검색 및 버튼 영역 */}
                        <div className="row mb-3">
                            <div className="col-md-6">
                                <div className="input-group mb-3">
                                    <input
                                        type="text"
                                        className="form-control"
                                        placeholder="검색어를 입력하세요"
                                        aria-label="검색어를 입력하세요"
                                    />
                                    <button className="btn btn-dark" type="button">
                                        검색
                                    </button>
                                </div>
                            </div>
                            <div className="col-md-6 text-end">
                                <button className="btn btn-primary">글쓰기</button>
                            </div>
                        </div>

                        {/* 게시글 목록 테이블 */}
                        <div className="card mb-4">
                            <div className="card-body p-0">
                                <div className="table-responsive">
                                    <table className="table table-hover mb-0">
                                        <thead className="bg-light">
                                        <tr>
                                            <th style={{width: "60%"}}>제목</th>
                                            <th style={{width: "15%"}}>농장</th>
                                            <th style={{width: "15%"}}>작성일</th>
                                            <th style={{width: "10%"}}>조회수</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {simplePostDTO.map((simplePostDTO) => (
                                            <tr key={simplePostDTO.postId}>
                                                <td>
                                                    <a href={`/board/${simplePostDTO.postId}`}
                                                       className="text-decoration-none text-dark">
                                                        {simplePostDTO.title}{""}
                                                        <span className="text-primary">
                                [{simplePostDTO.commentCount}]
                              </span>
                                                    </a>
                                                </td>
                                                <td>{simplePostDTO.farmName}</td>
                                                <td>{simplePostDTO.createdAt}</td>
                                                <td>{simplePostDTO.views}</td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>

                        {/* 페이지네이션 */}
                        <nav aria-label="페이지 네비게이션">
                            <ul className="pagination justify-content-center my-4">
                                <li className="page-item disabled">
                                    <a
                                        className="page-link"
                                        href="#"
                                        tabIndex={-1}
                                        aria-disabled="true"
                                    >
                                        이전
                                    </a>
                                </li>
                                <li className="page-item active" aria-current="page">
                                    <a className="page-link" href="#">
                                        1
                                    </a>
                                </li>
                                <li className="page-item">
                                    <a className="page-link" href="#">
                                        2
                                    </a>
                                </li>
                                <li className="page-item">
                                    <a className="page-link" href="#">
                                        3
                                    </a>
                                </li>
                                <li className="page-item">
                                    <a className="page-link" href="#">
                                        다음
                                    </a>
                                </li>
                            </ul>
                        </nav>
                    </div>

                    {/* 사이드바 */}
                    <div className="col-lg-4">
                        {/* 인기 게시글 */}
                        <div className="card mb-4">
                            <div className="card-header">인기 게시글</div>
                            <div className="card-body">
                                <ul className="list-unstyled mb-0">
                                    {simplePostDTO
                                        .slice(0, 5)
                                        .sort((a, b) => b.views - a.views)
                                        .map((simplePostDTO) => (
                                            <li key={simplePostDTO.postId} className="border-bottom pb-2 mb-2">
                                                <a
                                                    href={`/board/${simplePostDTO.postId}`}
                                                    className="text-decoration-none text-dark"
                                                >
                                                    <div className="d-flex justify-content-between align-items-center">
                                                        <span className="text-truncate" style={{maxWidth: "250px"}}>
                                                            {simplePostDTO.title}
                                                        </span>
                                                        <span
                                                            className="badge bg-light text-dark">{simplePostDTO.views}</span>
                                                    </div>
                                                </a>
                                            </li>
                                        ))}
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    );
}
